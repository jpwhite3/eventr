import React, { useState, useRef } from 'react';

interface ImageUploadProps {
    label: string;
    currentImageUrl?: string;
    onImageUpload: (imageUrl: string) => void;
    type: 'banner' | 'thumbnail';
    aspectRatio?: string;
    maxSize?: number; // in MB
}

const ImageUpload: React.FC<ImageUploadProps> = ({
    label,
    currentImageUrl,
    onImageUpload,
    type,
    aspectRatio = '16:9',
    maxSize = 10
}) => {
    const [isUploading, setIsUploading] = useState(false);
    const [previewUrl, setPreviewUrl] = useState<string | null>(currentImageUrl || null);
    const [error, setError] = useState<string | null>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];

    const validateFile = (file: File): string | null => {
        if (!allowedTypes.includes(file.type)) {
            return 'Please select a valid image file (JPEG, PNG, GIF, or WebP)';
        }
        
        if (file.size > maxSize * 1024 * 1024) {
            return `File size must be less than ${maxSize}MB`;
        }
        
        return null;
    };

    const handleFileSelect = async (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        if (!file) return;

        setError(null);

        const validationError = validateFile(file);
        if (validationError) {
            setError(validationError);
            return;
        }

        // Create preview immediately
        const objectUrl = URL.createObjectURL(file);
        setPreviewUrl(objectUrl);

        setIsUploading(true);

        try {
            const formData = new FormData();
            formData.append('file', file);

            const response = await fetch('/api/storage/upload', {
                method: 'POST',
                body: formData,
            });

            if (!response.ok) {
                let errorMessage = 'Upload failed';
                try {
                    const errorText = await response.text();
                    errorMessage = errorText || `Upload failed with status ${response.status}`;
                } catch {
                    // Response parsing failed
                    errorMessage = `Upload failed: ${response.statusText} (${response.status})`;
                }
                throw new Error(errorMessage);
            }

            // Response is plain text containing the file URL
            const fileUrl = await response.text();
            onImageUpload(fileUrl);
            
            // Clean up the object URL since we have the real URL now
            URL.revokeObjectURL(objectUrl);
            setPreviewUrl(fileUrl);
            
        } catch (err) {
            console.error('Upload failed:', err);
            setError(err instanceof Error ? err.message : 'Upload failed');
            // Revert to original image on error
            setPreviewUrl(currentImageUrl || null);
            URL.revokeObjectURL(objectUrl);
        } finally {
            setIsUploading(false);
        }
    };

    const handleRemoveImage = () => {
        // Note: Backend doesn't currently have a delete endpoint
        // Files are stored in S3/LocalStack and managed by the backend
        // Simply remove the reference from the UI
        setPreviewUrl(null);
        onImageUpload('');
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    const getRecommendedSize = () => {
        switch (type) {
            case 'banner':
                return aspectRatio === '16:9' ? '1920x1080px' : '1200x600px';
            case 'thumbnail':
                return '400x400px';
            default:
                return '800x600px';
        }
    };

    return (
        <div className="image-upload-container">
            <label className="form-label">{label}</label>
            <div className="form-text mb-2">
                Recommended size: {getRecommendedSize()} • Max file size: {maxSize}MB
            </div>

            {previewUrl ? (
                <div className="image-preview-container mb-3">
                    <div className={`image-preview ${type === 'banner' ? 'banner-preview' : 'thumbnail-preview'}`}>
                        <img 
                            src={previewUrl} 
                            alt="Preview" 
                            className="preview-image"
                        />
                        {isUploading && (
                            <div className="upload-overlay">
                                <div className="spinner-border text-light" role="status">
                                    <span className="visually-hidden">Uploading...</span>
                                </div>
                                <div className="text-light mt-2">Uploading...</div>
                            </div>
                        )}
                    </div>
                    <div className="image-actions mt-2">
                        <button
                            type="button"
                            className="btn btn-sm btn-outline-primary me-2"
                            onClick={() => fileInputRef.current?.click()}
                            disabled={isUploading}
                        >
                            📷 Change Image
                        </button>
                        <button
                            type="button"
                            className="btn btn-sm btn-outline-danger"
                            onClick={handleRemoveImage}
                            disabled={isUploading}
                        >
                            🗑️ Remove
                        </button>
                    </div>
                </div>
            ) : (
                <div className="upload-area" onClick={() => fileInputRef.current?.click()}>
                    <div className={`upload-placeholder ${type === 'banner' ? 'banner-placeholder' : 'thumbnail-placeholder'}`}>
                        <div className="upload-content">
                            <div className="upload-icon">📷</div>
                            <div className="upload-text">
                                <strong>Click to upload {type}</strong>
                                <div className="upload-hint">
                                    Drag & drop or click to select
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            <input
                ref={fileInputRef}
                type="file"
                accept={allowedTypes.join(',')}
                onChange={handleFileSelect}
                className="d-none"
                disabled={isUploading}
            />

            {error && (
                <div className="alert alert-danger mt-2 py-2">
                    <small>⚠️ {error}</small>
                </div>
            )}

            <style dangerouslySetInnerHTML={{
                __html: `
                .image-upload-container {
                    margin-bottom: 1rem;
                }

                .upload-area {
                    cursor: pointer;
                    border: 2px dashed #dee2e6;
                    border-radius: 8px;
                    transition: all 0.2s ease;
                }

                .upload-area:hover {
                    border-color: #007bff;
                    background-color: #f8f9fa;
                }

                .upload-placeholder {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    padding: 2rem;
                    text-align: center;
                    color: #6c757d;
                }

                .banner-placeholder {
                    min-height: 200px;
                    aspect-ratio: 16/9;
                }

                .thumbnail-placeholder {
                    min-height: 150px;
                    aspect-ratio: 1/1;
                    max-width: 200px;
                }

                .upload-content {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                }

                .upload-icon {
                    font-size: 3rem;
                    margin-bottom: 1rem;
                    opacity: 0.6;
                }

                .upload-text {
                    line-height: 1.4;
                }

                .upload-hint {
                    font-size: 0.875rem;
                    color: #adb5bd;
                    margin-top: 0.25rem;
                }

                .image-preview-container {
                    position: relative;
                }

                .image-preview {
                    position: relative;
                    border-radius: 8px;
                    overflow: hidden;
                    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                }

                .banner-preview {
                    aspect-ratio: 16/9;
                    max-height: 300px;
                }

                .thumbnail-preview {
                    aspect-ratio: 1/1;
                    max-width: 200px;
                    max-height: 200px;
                }

                .preview-image {
                    width: 100%;
                    height: 100%;
                    object-fit: cover;
                    display: block;
                }

                .upload-overlay {
                    position: absolute;
                    top: 0;
                    left: 0;
                    right: 0;
                    bottom: 0;
                    background: rgba(0,0,0,0.7);
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    color: white;
                }

                .image-actions {
                    display: flex;
                    gap: 0.5rem;
                }

                @media (max-width: 768px) {
                    .upload-placeholder {
                        padding: 1rem;
                    }
                    
                    .upload-icon {
                        font-size: 2rem;
                        margin-bottom: 0.5rem;
                    }
                    
                    .banner-placeholder {
                        min-height: 120px;
                    }
                    
                    .thumbnail-placeholder {
                        min-height: 100px;
                    }
                }
                `
            }} />
        </div>
    );
};

export default ImageUpload;