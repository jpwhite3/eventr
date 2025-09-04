import React, { useState, useEffect, useCallback } from 'react';
import apiClient from '../api/apiClient';

interface QRCodeDisplayProps {
    eventId: string;
    registrationId: string;
    userName: string;
    userEmail: string;
}

interface QRCodeResponse {
    qrCodeBase64: string;
    qrData: string;
    expiresAt?: string;
}

const QRCodeDisplay: React.FC<QRCodeDisplayProps> = ({ eventId, registrationId, userName, userEmail }) => {
    const [qrCode, setQrCode] = useState<QRCodeResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const generateQRCode = useCallback(async () => {
        setLoading(true);
        setError(null);
        
        try {
            const response = await apiClient.get(
                `/checkin/qr/badge/event/${eventId}/user/${registrationId}?userName=${encodeURIComponent(userName)}`
            );
            setQrCode(response.data);
        } catch (err) {
            setError('Failed to generate QR code');
            console.error('QR code generation error:', err);
        } finally {
            setLoading(false);
        }
    }, [eventId, registrationId, userName]);

    const downloadQRCode = async () => {
        try {
            const response = await apiClient.get(
                `/checkin/qr/badge/event/${eventId}/user/${registrationId}/image?userName=${encodeURIComponent(userName)}`,
                { responseType: 'blob' }
            );
            
            const blob = new Blob([response.data], { type: 'image/png' });
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `${userName.replace(/[^a-z0-9]/gi, '_')}_badge.png`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
        } catch (err) {
            console.error('Failed to download QR code:', err);
        }
    };

    useEffect(() => {
        if (eventId && registrationId && userName) {
            generateQRCode();
        }
    }, [eventId, registrationId, userName, generateQRCode]);

    if (loading) {
        return (
            <div className="text-center p-4">
                <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Generating QR code...</span>
                </div>
                <p className="mt-2">Generating your check-in QR code...</p>
            </div>
        );
    }

    if (error) {
        return (
            <div className="alert alert-warning">
                <p className="mb-2">{error}</p>
                <button className="btn btn-outline-primary btn-sm" onClick={generateQRCode}>
                    Try Again
                </button>
            </div>
        );
    }

    if (!qrCode) {
        return (
            <div className="text-center p-4">
                <button className="btn btn-primary" onClick={generateQRCode}>
                    Generate Check-in QR Code
                </button>
            </div>
        );
    }

    return (
        <div className="card">
            <div className="card-header bg-primary text-white">
                <h5 className="mb-0">
                    <i className="bi bi-qr-code me-2"></i>
                    Your Check-in QR Code
                </h5>
            </div>
            <div className="card-body text-center">
                <div className="mb-3">
                    <img 
                        src={`data:image/png;base64,${qrCode.qrCodeBase64}`}
                        alt="Check-in QR Code"
                        className="img-fluid"
                        style={{ maxWidth: '200px', maxHeight: '200px' }}
                    />
                </div>
                
                <div className="mb-3">
                    <h6>How to use this QR code:</h6>
                    <ul className="list-unstyled text-muted small">
                        <li>• Show this code to event staff for quick check-in</li>
                        <li>• Save it to your phone's photos for offline access</li>
                        <li>• Print it out if you prefer a paper confirmation</li>
                    </ul>
                </div>

                <div className="d-grid gap-2">
                    <button 
                        className="btn btn-outline-primary btn-sm" 
                        onClick={downloadQRCode}
                    >
                        <i className="bi bi-download me-1"></i>
                        Download QR Code Badge
                    </button>
                    
                    <button 
                        className="btn btn-outline-secondary btn-sm" 
                        onClick={generateQRCode}
                    >
                        <i className="bi bi-arrow-clockwise me-1"></i>
                        Refresh Code
                    </button>
                </div>

                {qrCode.expiresAt && (
                    <small className="text-muted mt-2 d-block">
                        Code expires: {new Date(qrCode.expiresAt).toLocaleString()}
                    </small>
                )}
            </div>
        </div>
    );
};

export default QRCodeDisplay;