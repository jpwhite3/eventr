import React, { useState, useRef, useEffect } from 'react';

interface QRScannerProps {
    onScan: (qrCode: string) => void;
    isActive: boolean;
    onError?: (error: string) => void;
}

const QRScanner: React.FC<QRScannerProps> = ({ onScan, isActive, onError }) => {
    const videoRef = useRef<HTMLVideoElement>(null);
    const canvasRef = useRef<HTMLCanvasElement>(null);
    const [isSupported, setIsSupported] = useState(true);
    const [isScanning, setIsScanning] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [stream, setStream] = useState<MediaStream | null>(null);
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

    const startCamera = async () => {
        try {
            setError(null);
            setIsScanning(true);

            const constraints = {
                video: {
                    facingMode: 'environment', // Use back camera if available
                    width: { ideal: 640 },
                    height: { ideal: 480 }
                }
            };

            const mediaStream = await navigator.mediaDevices.getUserMedia(constraints);
            setStream(mediaStream);

            if (videoRef.current) {
                videoRef.current.srcObject = mediaStream;
                videoRef.current.play();
                startScanning();
            }
        } catch (err) {
            const errorMessage = `Camera access denied or unavailable: ${err}`;
            setError(errorMessage);
            onError?.(errorMessage);
            setIsScanning(false);
        }
    };

    const stopCamera = () => {
        if (scanIntervalRef.current) {
            clearInterval(scanIntervalRef.current);
            scanIntervalRef.current = null;
        }

        if (stream) {
            stream.getTracks().forEach(track => track.stop());
            setStream(null);
        }

        setIsScanning(false);
    };

    const startScanning = () => {
        if (!videoRef.current || !canvasRef.current) return;

        scanIntervalRef.current = setInterval(() => {
            scanForQRCode();
        }, 500); // Scan every 500ms
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

    // Simple QR code detection (placeholder - would use jsQR or similar library)
    const detectQRCode = (imageData: ImageData): string | null => {
        // This is a placeholder. In a real implementation, you would use:
        // import jsQR from 'jsqr';
        // const code = jsQR(imageData.data, imageData.width, imageData.height);
        // return code ? code.data : null;
        
        // For demo purposes, we'll simulate QR detection
        return null;
    };

    const handleManualInput = (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        const formData = new FormData(e.currentTarget);
        const qrCode = formData.get('qrCode') as string;
        if (qrCode.trim()) {
            onScan(qrCode.trim());
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
                                maxWidth: '400px',
                                height: 'auto',
                                border: '2px solid #007bff',
                                borderRadius: '8px'
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
                            {isScanning ? (
                                <span className="badge bg-success">
                                    📹 Scanning for QR codes...
                                </span>
                            ) : (
                                <span className="badge bg-secondary">
                                    ⏸️ Scanner inactive
                                </span>
                            )}
                        </div>
                    </>
                )}
            </div>
            
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

                @media (max-width: 768px) {
                    .scanner-frame {
                        width: 200px;
                        height: 200px;
                    }
                    
                    .scanner-video {
                        max-width: 300px;
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