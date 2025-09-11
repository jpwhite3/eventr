import React, { useState, useRef, useEffect, useCallback } from 'react';
import jsQR from 'jsqr';
import apiClient from '../api/apiClient';

interface QRScannerProps {
    onScan: (qrCode: string) => void;
    isActive: boolean;
    eventId?: string;
    onError?: (error: string) => void;
    onCheckInSuccess?: (result: any) => void;
}

interface CheckInResult {
    id: string;
    userName: string;
    userEmail: string;
    checkedInAt: string;
    status: string;
}

const QRScanner: React.FC<QRScannerProps> = ({ onScan, isActive, eventId, onError, onCheckInSuccess }) => {
    const videoRef = useRef<HTMLVideoElement>(null);
    const canvasRef = useRef<HTMLCanvasElement>(null);
    const [isSupported, setIsSupported] = useState(true);
    const [isScanning, setIsScanning] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [stream, setStream] = useState<MediaStream | null>(null);
    const [offlineQueue, setOfflineQueue] = useState<string[]>([]);
    const [checkInResult, setCheckInResult] = useState<CheckInResult | null>(null);
    const [isProcessing, setIsProcessing] = useState(false);
    const [facingMode, setFacingMode] = useState<'user' | 'environment'>('environment');
    const [availableCameras, setAvailableCameras] = useState<MediaDeviceInfo[]>([]);
    const scanIntervalRef = useRef<NodeJS.Timeout | null>(null);

    useEffect(() => {
        // Check if browser supports camera access
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            setIsSupported(false);
            setError('Camera not supported in this browser');
            return;
        }

        if (isActive) {
            startCamera();
        } else {
            stopCamera();
        }

        return () => {
            stopCamera();
        };
    }, [isActive]);

    // Load offline queue and enumerate cameras on mount
    useEffect(() => {
        const savedQueue = localStorage.getItem('qr_offline_queue');
        if (savedQueue) {
            setOfflineQueue(JSON.parse(savedQueue));
        }
        
        // Enumerate available cameras
        enumerateCameras();
    }, []);

    const enumerateCameras = async () => {
        try {
            const devices = await navigator.mediaDevices.enumerateDevices();
            const cameras = devices.filter(device => device.kind === 'videoinput');
            setAvailableCameras(cameras);
        } catch (error) {
            console.log('Could not enumerate cameras:', error);
        }
    };

    // Auto-sync when coming back online
    useEffect(() => {
        const handleOnline = () => {
            if (offlineQueue.length > 0) {
                syncOfflineQueue();
            }
        };

        window.addEventListener('online', handleOnline);
        return () => window.removeEventListener('online', handleOnline);
    }, [offlineQueue]);

    const startCamera = useCallback(async () => {
        try {
            setError(null);
            setIsScanning(true);

            // Enhanced mobile-friendly camera constraints
            const constraints = {
                video: {
                    facingMode: facingMode, // Use selected camera
                    width: { 
                        min: 320,
                        ideal: 800,
                        max: 1920
                    },
                    height: { 
                        min: 240,
                        ideal: 600,
                        max: 1080
                    },
                    frameRate: { ideal: 30, max: 30 },
                    aspectRatio: { ideal: 4/3 },
                    focusMode: 'continuous',
                    exposureMode: 'continuous',
                    whiteBalanceMode: 'continuous',
                    // Enhanced mobile camera settings
                    zoom: { ideal: 1.0 },
                    torch: false // Disable torch by default
                }
            };

            let mediaStream;
            
            try {
                // Try with enhanced constraints first
                mediaStream = await navigator.mediaDevices.getUserMedia(constraints);
            } catch (advancedErr) {
                // Fallback to basic constraints if advanced features aren't supported
                const basicConstraints = {
                    video: {
                        facingMode: facingMode,
                        width: { ideal: 640 },
                        height: { ideal: 480 }
                    }
                };
                mediaStream = await navigator.mediaDevices.getUserMedia(basicConstraints);
            }

            setStream(mediaStream);

            if (videoRef.current) {
                videoRef.current.srcObject = mediaStream;
                
                // Enhanced video element setup for mobile
                videoRef.current.setAttribute('playsinline', 'true');
                videoRef.current.setAttribute('webkit-playsinline', 'true');
                videoRef.current.muted = true;
                videoRef.current.autoplay = true;
                
                await videoRef.current.play();
                
                // Start scanning after video is ready
                videoRef.current.addEventListener('loadedmetadata', () => {
                    startScanning();
                });
            }
        } catch (err) {
            let errorMessage = 'Camera access denied or unavailable';
            
            if (err instanceof Error) {
                if (err.name === 'NotAllowedError') {
                    errorMessage = 'Camera permission denied. Please allow camera access and try again.';
                } else if (err.name === 'NotFoundError') {
                    errorMessage = 'No camera found. Please ensure your device has a camera.';
                } else if (err.name === 'NotSupportedError') {
                    errorMessage = 'Camera not supported by your browser.';
                } else {
                    errorMessage = `Camera error: ${err.message}`;
                }
            }
            
            setError(errorMessage);
            onError?.(errorMessage);
            setIsScanning(false);
        }
    }, [facingMode]);

    const stopCamera = useCallback(() => {
        if (scanIntervalRef.current) {
            clearInterval(scanIntervalRef.current);
            scanIntervalRef.current = null;
        }

        if (stream) {
            stream.getTracks().forEach(track => track.stop());
            setStream(null);
        }

        setIsScanning(false);
    }, [stream]);

    const startScanning = () => {
        if (!videoRef.current || !canvasRef.current) return;

        scanIntervalRef.current = setInterval(() => {
            scanForQRCode();
        }, 300); // Scan every 300ms for better responsiveness
    };

    const scanForQRCode = () => {
        const video = videoRef.current;
        const canvas = canvasRef.current;

        if (!video || !canvas || video.readyState !== video.HAVE_ENOUGH_DATA) {
            return;
        }

        const context = canvas.getContext('2d');
        if (!context) return;

        // Set canvas size to video size
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;

        // Draw video frame to canvas
        context.drawImage(video, 0, 0, canvas.width, canvas.height);

        try {
            // Get image data
            const imageData = context.getImageData(0, 0, canvas.width, canvas.height);
            
            // Try to decode QR code using a simple pattern detection
            // In a real implementation, you'd use a proper QR code library like jsQR
            const qrCode = detectQRCode(imageData);
            
            if (qrCode) {
                onScan(qrCode);
                // Stop scanning temporarily to prevent multiple scans
                if (scanIntervalRef.current) {
                    clearInterval(scanIntervalRef.current);
                    setTimeout(() => {
                        if (isActive) startScanning();
                    }, 2000);
                }
            }
        } catch (err) {
            // Ignore scan errors, continue trying
        }
    };

    // Enhanced QR code detection using jsQR library
    const detectQRCode = (imageData: ImageData): string | null => {
        try {
            const code = jsQR(imageData.data, imageData.width, imageData.height, {
                inversionAttempts: 'dontInvert',
            });
            
            if (code) {
                console.log('QR Code detected:', code.data);
                return code.data;
            }
            
            // Try with inversion if first attempt fails
            const invertedCode = jsQR(imageData.data, imageData.width, imageData.height, {
                inversionAttempts: 'attemptBoth',
            });
            
            if (invertedCode) {
                console.log('QR Code detected (inverted):', invertedCode.data);
                return invertedCode.data;
            }
            
            return null;
        } catch (error) {
            console.warn('QR detection error:', error);
            return null;
        }
    };

    // Process QR code for check-in
    const processCheckIn = async (qrData: string) => {
        if (isProcessing) return;

        setIsProcessing(true);
        setError(null);
        setCheckInResult(null);

        try {
            const response = await apiClient.post('/checkin/qr', {
                qrData: qrData,
                eventId: eventId,
                scannedAt: new Date().toISOString(),
                deviceId: getDeviceId()
            });

            const result: CheckInResult = response.data;
            setCheckInResult(result);
            
            if (onCheckInSuccess) {
                onCheckInSuccess(result);
            }

            // Clear the result after 3 seconds
            setTimeout(() => {
                setCheckInResult(null);
            }, 3000);

        } catch (err: any) {
            const errorMessage = err.response?.data?.message || 'Check-in failed';
            setError(errorMessage);
            
            // Add to offline queue if network error
            if (!navigator.onLine) {
                addToOfflineQueue(qrData);
            }
            
            if (onError) {
                onError(errorMessage);
            }
        } finally {
            setIsProcessing(false);
        }
    };

    // Add to offline queue
    const addToOfflineQueue = (qrData: string) => {
        const newQueue = [...offlineQueue, qrData];
        setOfflineQueue(newQueue);
        localStorage.setItem('qr_offline_queue', JSON.stringify(newQueue));
    };

    // Sync offline queue
    const syncOfflineQueue = useCallback(async () => {
        if (offlineQueue.length === 0) return;

        try {
            await apiClient.post('/checkin/sync', 
                offlineQueue.map(qrData => ({
                    qrData,
                    eventId,
                    scannedAt: new Date().toISOString(),
                    deviceId: getDeviceId()
                }))
            );

            // Clear successful syncs
            setOfflineQueue([]);
            localStorage.removeItem('qr_offline_queue');
            
        } catch (err) {
            console.error('Failed to sync offline check-ins:', err);
        }
    }, [offlineQueue, eventId]);

    // Get device ID for tracking
    const getDeviceId = (): string => {
        let deviceId = localStorage.getItem('device_id');
        if (!deviceId) {
            deviceId = `device_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
            localStorage.setItem('device_id', deviceId);
        }
        return deviceId;
    };

    const switchCamera = async () => {
        if (availableCameras.length < 2) return;
        
        const newFacingMode = facingMode === 'environment' ? 'user' : 'environment';
        setFacingMode(newFacingMode);
        
        // Restart camera with new facing mode
        if (isScanning) {
            stopCamera();
            setTimeout(() => {
                startCamera();
            }, 100);
        }
    };

    const handleManualInput = (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        const formData = new FormData(e.currentTarget);
        const qrCode = formData.get('qrCode') as string;
        if (qrCode.trim()) {
            if (eventId) {
                processCheckIn(qrCode.trim());
            } else {
                onScan(qrCode.trim());
            }
            (e.target as HTMLFormElement).reset();
        }
    };

    if (!isSupported) {
        return (
            <div className="text-center p-4">
                <div className="alert alert-warning">
                    <h5>📱 Camera Not Supported</h5>
                    <p>Your device or browser doesn't support camera access for QR scanning.</p>
                    <p>You can still manually enter QR code URLs below.</p>
                </div>
                <ManualQRInput onSubmit={handleManualInput} />
            </div>
        );
    }

    return (
        <div className="qr-scanner">
            <div className="scanner-container position-relative">
                {error ? (
                    <div className="alert alert-danger">
                        <h6>❌ Scanner Error</h6>
                        <p>{error}</p>
                        <button 
                            className="btn btn-primary btn-sm"
                            onClick={startCamera}
                        >
                            🔄 Retry Camera
                        </button>
                    </div>
                ) : (
                    <>
                        <video
                            ref={videoRef}
                            className="scanner-video"
                            playsInline
                            muted
                            style={{
                                width: '100%',
                                maxWidth: '100%',
                                height: 'auto',
                                minHeight: '250px',
                                maxHeight: '400px',
                                border: '3px solid #007bff',
                                borderRadius: '16px',
                                objectFit: 'cover',
                                backgroundColor: '#000'
                            }}
                        />
                        
                        <canvas
                            ref={canvasRef}
                            style={{ display: 'none' }}
                        />
                        
                        {/* Scanner overlay */}
                        <div className="scanner-overlay position-absolute top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center">
                            <div className="scanner-frame">
                                <div className="scanner-corners"></div>
                                {isScanning && (
                                    <div className="scanner-line"></div>
                                )}
                            </div>
                        </div>
                        
                        <div className="scanner-status text-center mt-3">
                            <div className="d-flex justify-content-center gap-2 flex-wrap mb-3">
                                {isScanning ? (
                                    <span className="badge bg-success">
                                        📹 Scanning for QR codes...
                                    </span>
                                ) : (
                                    <span className="badge bg-secondary">
                                        ⏸️ Scanner inactive
                                    </span>
                                )}
                                
                                {offlineQueue.length > 0 && (
                                    <button 
                                        className="badge bg-warning text-decoration-none border-0"
                                        onClick={syncOfflineQueue}
                                        disabled={!navigator.onLine}
                                    >
                                        📱 {offlineQueue.length} offline check-ins
                                    </button>
                                )}
                                
                                {!navigator.onLine && (
                                    <span className="badge bg-warning">
                                        📵 Offline Mode
                                    </span>
                                )}
                                
                                {isProcessing && (
                                    <span className="badge bg-primary">
                                        ⏳ Processing...
                                    </span>
                                )}
                            </div>
                            
                            {/* Mobile Camera Controls */}
                            {availableCameras.length > 1 && isScanning && (
                                <div className="camera-controls">
                                    <button 
                                        className="btn btn-outline-primary btn-sm me-2"
                                        onClick={switchCamera}
                                        title={`Switch to ${facingMode === 'environment' ? 'front' : 'back'} camera`}
                                    >
                                        🔄 {facingMode === 'environment' ? '🤳' : '📷'} Switch Camera
                                    </button>
                                </div>
                            )}
                        </div>
                    </>
                )}
            </div>

            {/* Check-in result display */}
            {checkInResult && (
                <div className="alert alert-success mt-3">
                    <h6>✅ Check-in Successful!</h6>
                    <p className="mb-1"><strong>{checkInResult.userName}</strong></p>
                    <p className="mb-1">{checkInResult.userEmail}</p>
                    <small className="text-muted">
                        Checked in at {new Date(checkInResult.checkedInAt).toLocaleString()}
                    </small>
                </div>
            )}
            
            {/* Manual input fallback */}
            <div className="manual-input mt-4">
                <h6>💻 Manual Entry</h6>
                <p className="small text-muted">Can't scan? Enter the QR code URL manually:</p>
                <ManualQRInput onSubmit={handleManualInput} />
            </div>

            <style dangerouslySetInnerHTML={{
                __html: `
                .scanner-container {
                    max-width: 400px;
                    margin: 0 auto;
                }

                .scanner-overlay {
                    pointer-events: none;
                }

                .scanner-frame {
                    width: 250px;
                    height: 250px;
                    position: relative;
                    border: 2px solid rgba(255, 255, 255, 0.8);
                    border-radius: 12px;
                    background: rgba(0, 0, 0, 0.1);
                }

                .scanner-corners::before,
                .scanner-corners::after {
                    content: '';
                    position: absolute;
                    width: 30px;
                    height: 30px;
                    border: 3px solid #007bff;
                }

                .scanner-corners::before {
                    top: -3px;
                    left: -3px;
                    border-right: none;
                    border-bottom: none;
                    border-top-left-radius: 12px;
                }

                .scanner-corners::after {
                    bottom: -3px;
                    right: -3px;
                    border-left: none;
                    border-top: none;
                    border-bottom-right-radius: 12px;
                }

                .scanner-line {
                    position: absolute;
                    top: 0;
                    left: 0;
                    right: 0;
                    height: 2px;
                    background: linear-gradient(90deg, transparent, #007bff, transparent);
                    animation: scanLine 2s infinite;
                }

                @keyframes scanLine {
                    0% { top: 0; opacity: 1; }
                    50% { top: 50%; opacity: 0.8; }
                    100% { top: 100%; opacity: 0; }
                }

                .manual-input {
                    border-top: 1px solid #dee2e6;
                    padding-top: 1rem;
                }

                .camera-controls {
                    margin-top: 0.5rem;
                }

                .camera-controls button {
                    font-size: 0.9rem;
                    padding: 0.5rem 1rem;
                    border-radius: 20px;
                    font-weight: 500;
                }

                @media (max-width: 768px) {
                    .scanner-container {
                        max-width: 100%;
                        padding: 0;
                    }

                    .scanner-frame {
                        width: 200px;
                        height: 200px;
                    }
                    
                    .scanner-video {
                        max-width: 100%;
                        border-radius: 12px;
                        border-width: 2px;
                    }

                    .camera-controls button {
                        font-size: 0.85rem;
                        padding: 0.4rem 0.8rem;
                    }
                }

                @media (max-width: 480px) {
                    .scanner-frame {
                        width: 150px;
                        height: 150px;
                    }
                    
                    .camera-controls {
                        margin-top: 1rem;
                    }

                    .camera-controls button {
                        width: 100%;
                        max-width: 200px;
                    }
                }
                `
            }} />
        </div>
    );
};

const ManualQRInput: React.FC<{ onSubmit: (e: React.FormEvent<HTMLFormElement>) => void }> = ({ onSubmit }) => (
    <form onSubmit={onSubmit} className="d-flex gap-2">
        <input
            type="url"
            name="qrCode"
            className="form-control form-control-sm"
            placeholder="Paste QR code URL here..."
            required
        />
        <button type="submit" className="btn btn-outline-primary btn-sm">
            ✅ Submit
        </button>
    </form>
);

export default QRScanner;