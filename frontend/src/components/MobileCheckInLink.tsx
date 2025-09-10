import React, { useState } from 'react';
import { QRCodeCanvas } from 'qrcode.react';

interface MobileCheckInLinkProps {
    eventId: string;
    eventName: string;
    sessionId?: string;
    sessionName?: string;
    defaultStaff?: string;
    defaultRole?: string;
    className?: string;
}

const MobileCheckInLink: React.FC<MobileCheckInLinkProps> = ({
    eventId,
    eventName,
    sessionId,
    sessionName,
    defaultStaff = 'Event Staff',
    defaultRole = 'Staff',
    className = ''
}) => {
    const [showQR, setShowQR] = useState(false);
    const [staffName, setStaffName] = useState(defaultStaff);
    const [staffRole, setStaffRole] = useState(defaultRole);
    const [customized, setCustomized] = useState(false);

    // Generate mobile check-in URL
    const generateMobileUrl = () => {
        const baseUrl = window.location.origin;
        const url = new URL(`${baseUrl}/mobile-checkin/${eventId}`);
        
        if (sessionId) {
            url.searchParams.set('sessionId', sessionId);
        }
        
        url.searchParams.set('staff', encodeURIComponent(staffName));
        url.searchParams.set('role', encodeURIComponent(staffRole));
        
        return url.toString();
    };

    const mobileUrl = generateMobileUrl();

    const copyToClipboard = async () => {
        try {
            await navigator.clipboard.writeText(mobileUrl);
            // You could show a toast notification here
            alert('Mobile check-in URL copied to clipboard!');
        } catch (error) {
            console.error('Failed to copy URL:', error);
            // Fallback: select text
            const textArea = document.createElement('textarea');
            textArea.value = mobileUrl;
            document.body.appendChild(textArea);
            textArea.select();
            document.execCommand('copy');
            document.body.removeChild(textArea);
            alert('Mobile check-in URL copied to clipboard!');
        }
    };

    const shareUrl = async () => {
        if (navigator.share) {
            try {
                await navigator.share({
                    title: `Check-in to ${eventName}`,
                    text: sessionName ? 
                        `Mobile check-in for "${sessionName}" session` : 
                        `Mobile check-in for "${eventName}"`,
                    url: mobileUrl
                });
            } catch (error) {
                console.error('Share failed:', error);
                copyToClipboard();
            }
        } else {
            copyToClipboard();
        }
    };

    return (
        <div className={`mobile-checkin-link-generator ${className}`}>
            <div className="card">
                <div className="card-header">
                    <h6 className="card-title mb-0">
                        📱 Mobile Check-In Link
                    </h6>
                </div>
                <div className="card-body">
                    <div className="mb-3">
                        <div className="event-info">
                            <strong>Event:</strong> {eventName}
                            {sessionName && (
                                <>
                                    <br />
                                    <strong>Session:</strong> {sessionName}
                                </>
                            )}
                        </div>
                    </div>

                    {/* Staff Customization */}
                    <div className="staff-customization mb-3">
                        <div className="d-flex justify-content-between align-items-center mb-2">
                            <small className="text-muted">Staff Information</small>
                            <button 
                                className="btn btn-link btn-sm p-0"
                                onClick={() => setCustomized(!customized)}
                            >
                                {customized ? 'Hide' : 'Customize'}
                            </button>
                        </div>
                        
                        {customized && (
                            <div className="row">
                                <div className="col-md-6 mb-2">
                                    <input
                                        type="text"
                                        className="form-control form-control-sm"
                                        placeholder="Staff Name"
                                        value={staffName}
                                        onChange={(e) => setStaffName(e.target.value)}
                                    />
                                </div>
                                <div className="col-md-6 mb-2">
                                    <input
                                        type="text"
                                        className="form-control form-control-sm"
                                        placeholder="Staff Role"
                                        value={staffRole}
                                        onChange={(e) => setStaffRole(e.target.value)}
                                    />
                                </div>
                            </div>
                        )}
                    </div>

                    {/* URL Display */}
                    <div className="url-display mb-3">
                        <div className="input-group">
                            <input
                                type="text"
                                className="form-control form-control-sm"
                                value={mobileUrl}
                                readOnly
                                style={{ fontSize: '0.8rem' }}
                            />
                            <button
                                className="btn btn-outline-secondary btn-sm"
                                onClick={copyToClipboard}
                                title="Copy URL"
                            >
                                📋
                            </button>
                        </div>
                    </div>

                    {/* Action Buttons */}
                    <div className="action-buttons d-flex gap-2 flex-wrap">
                        <button
                            className="btn btn-primary btn-sm"
                            onClick={shareUrl}
                        >
                            📤 Share Link
                        </button>
                        
                        <button
                            className="btn btn-outline-primary btn-sm"
                            onClick={() => window.open(mobileUrl, '_blank')}
                        >
                            🔗 Open Mobile View
                        </button>
                        
                        <button
                            className="btn btn-outline-secondary btn-sm"
                            onClick={() => setShowQR(!showQR)}
                        >
                            📱 {showQR ? 'Hide' : 'Show'} QR Code
                        </button>
                    </div>

                    {/* QR Code */}
                    {showQR && (
                        <div className="qr-code-container mt-3 text-center">
                            <div className="qr-wrapper d-inline-block p-3 bg-white border rounded">
                                <QRCodeCanvas
                                    value={mobileUrl}
                                    size={200}
                                    level="M"
                                    includeMargin={true}
                                />
                                <div className="mt-2 small text-muted">
                                    Scan to access mobile check-in
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Usage Instructions */}
                    <div className="usage-instructions mt-3">
                        <small className="text-muted">
                            <strong>📖 Instructions:</strong>
                            <ul className="mb-0 mt-1">
                                <li>Share this link with event staff for mobile check-in access</li>
                                <li>Staff can use their mobile devices to scan QR codes or search attendees</li>
                                <li>Works offline - check-ins will sync when connection returns</li>
                                <li>Best used on mobile devices for optimal experience</li>
                            </ul>
                        </small>
                    </div>
                </div>
            </div>

            <style dangerouslySetInnerHTML={{
                __html: `
                .mobile-checkin-link-generator .card {
                    border: 1px solid #e9ecef;
                    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
                }
                
                .mobile-checkin-link-generator .event-info {
                    background: #f8f9fa;
                    padding: 0.75rem;
                    border-radius: 0.375rem;
                    font-size: 0.9rem;
                }
                
                .mobile-checkin-link-generator .url-display input {
                    background: #f8f9fa;
                    border-right: none;
                }
                
                .mobile-checkin-link-generator .qr-wrapper {
                    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
                }
                
                .mobile-checkin-link-generator .usage-instructions ul {
                    font-size: 0.8rem;
                    padding-left: 1.2rem;
                }
                
                .mobile-checkin-link-generator .usage-instructions li {
                    margin-bottom: 0.25rem;
                }
                
                @media (max-width: 768px) {
                    .mobile-checkin-link-generator .action-buttons {
                        justify-content: stretch;
                    }
                    
                    .mobile-checkin-link-generator .action-buttons .btn {
                        flex: 1;
                    }
                }
                `
            }} />
        </div>
    );
};

export default MobileCheckInLink;