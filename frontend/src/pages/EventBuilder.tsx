import React, { useState, useEffect, ChangeEvent, FormEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import apiClient from '../api/apiClient';
import FormBuilder from '../components/FormBuilder';
import ImageUpload from '../components/ImageUpload';
import SessionBuilder from '../components/SessionBuilder';

// Interface for event form data
interface EventData {
    name: string;
    description: string;
    tags: string;
    capacity: number;
    waitlistEnabled: boolean;
    bannerImageUrl: string;
    thumbnailImageUrl: string;
    formData: string;
    
    // Event type and category
    eventType: string;
    category: string;
    
    // Location fields for in-person events
    venueName: string;
    address: string;
    city: string;
    state: string;
    zipCode: string;
    country: string;
    
    // Virtual event fields
    virtualUrl: string;
    dialInNumber: string;
    accessCode: string;
    
    // Registration settings
    requiresApproval: boolean;
    maxRegistrations: number;
    
    // Organizer information
    organizerName: string;
    organizerEmail: string;
    organizerPhone: string;
    organizerWebsite: string;
    
    // Event timing
    startDateTime: string;
    endDateTime: string;
    timezone: string;
    
    // Agenda/Schedule
    agenda: string;
    
    // Session configuration
    isMultiSession: boolean;
    allowSessionSelection: boolean;
}

// Interface for API event data (when tags is an array)
interface ApiEventData extends Omit<EventData, 'tags'> {
    tags?: string[];
}

const EventBuilder: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const [showMarkdownPreview, setShowMarkdownPreview] = useState<boolean>(false);
    const [event, setEvent] = useState<EventData>({
        name: '',
        description: '',
        tags: '',
        capacity: 0,
        waitlistEnabled: false,
        bannerImageUrl: '',
        thumbnailImageUrl: '',
        formData: '',
        
        // Event type and category
        eventType: 'IN_PERSON',
        category: '',
        
        // Location fields for in-person events
        venueName: '',
        address: '',
        city: '',
        state: '',
        zipCode: '',
        country: '',
        
        // Virtual event fields
        virtualUrl: '',
        dialInNumber: '',
        accessCode: '',
        
        // Registration settings
        requiresApproval: false,
        maxRegistrations: 0,
        
        // Organizer information
        organizerName: '',
        organizerEmail: '',
        organizerPhone: '',
        organizerWebsite: '',
        
        // Event timing
        startDateTime: '',
        endDateTime: '',
        timezone: 'UTC',
        
        // Agenda/Schedule
        agenda: '',
        
        // Session configuration
        isMultiSession: false,
        allowSessionSelection: false
    });

    useEffect(() => {
        if (id) {
            apiClient.get(`/events/${id}`).then(response => {
                const eventData: ApiEventData = response.data;
                setEvent({ ...eventData, tags: eventData.tags ? eventData.tags.join(', ') : '' });
            });
            apiClient.get(`/events/${id}/form`).then(response => {
                setEvent(prevEvent => ({ ...prevEvent, formData: response.data }));
            }).catch(err => {
                console.log("No form definition found, starting with a blank slate.");
            });
        }
    }, [id]);

    const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>): void => {
        const target = e.target as HTMLInputElement;
        const { name, value, type } = target;
        const checked = target.checked;
        setEvent(prevEvent => ({
            ...prevEvent,
            [name]: type === 'checkbox' ? checked : (type === 'number' ? parseFloat(value) || 0 : value)
        }));
    };

    const handleSubmit = (e: FormEvent<HTMLFormElement>): void => {
        e.preventDefault();

        // Validate JSON format for formData
        if (event.formData) {
            try {
                JSON.parse(event.formData);
            } catch (error) {
                alert("Invalid JSON format in the form definition. Please check and try again.");
                return;
            }
        }

        const payload = {
            ...event,
            tags: event.tags.split(',').map(tag => tag.trim())
        };

        const request = id ? apiClient.put(`/events/${id}`, payload) : apiClient.post('/events', payload);

        request.then(() => {
            navigate('/admin');
        }).catch(error => {
            console.error("Failed to save event", error);
            alert("Error saving event. Check console for details.");
        });
    };

    return (
        <div className="container mt-5">
            <h1 className="mb-4">{id ? 'Edit' : 'Create'} Event</h1>
            <form onSubmit={handleSubmit}>
                {/* Basic Event Information */}
                <div className="card mb-4">
                    <div className="card-header">
                        <h3>Basic Information</h3>
                    </div>
                    <div className="card-body">
                        <div className="row">
                            <div className="col-md-8 mb-3">
                                <label className="form-label">Event Name *</label>
                                <input type="text" className="form-control" name="name" value={event.name} onChange={handleChange} required />
                            </div>
                            <div className="col-md-4 mb-3">
                                <label className="form-label">Category</label>
                                <select className="form-select" name="category" value={event.category} onChange={handleChange}>
                                    <option value="">Select Category</option>
                                    <option value="MUSIC">Music</option>
                                    <option value="BUSINESS">Business</option>
                                    <option value="FOOD_DRINK">Food & Drink</option>
                                    <option value="NIGHTLIFE">Nightlife</option>
                                    <option value="PERFORMING_VISUAL_ARTS">Performing & Visual Arts</option>
                                    <option value="SPORTS_FITNESS">Sports & Fitness</option>
                                    <option value="TECHNOLOGY">Technology</option>
                                    <option value="EDUCATION">Education</option>
                                    <option value="COMMUNITY">Community</option>
                                    <option value="OTHER">Other</option>
                                </select>
                            </div>
                        </div>
                        <div className="mb-3">
                            <div className="d-flex justify-content-between align-items-center mb-2">
                                <label className="form-label mb-0">Description</label>
                                <div className="btn-group" role="group">
                                    <input
                                        type="radio"
                                        className="btn-check"
                                        name="descriptionView"
                                        id="write"
                                        checked={!showMarkdownPreview}
                                        onChange={() => setShowMarkdownPreview(false)}
                                    />
                                    <label className="btn btn-outline-secondary btn-sm" htmlFor="write">Write</label>
                                    <input
                                        type="radio"
                                        className="btn-check"
                                        name="descriptionView"
                                        id="preview"
                                        checked={showMarkdownPreview}
                                        onChange={() => setShowMarkdownPreview(true)}
                                    />
                                    <label className="btn btn-outline-secondary btn-sm" htmlFor="preview">Preview</label>
                                </div>
                            </div>
                            
                            {!showMarkdownPreview ? (
                                <div>
                                    <textarea 
                                        className="form-control" 
                                        name="description" 
                                        value={event.description} 
                                        onChange={handleChange} 
                                        rows={8} 
                                        placeholder="Tell attendees what to expect at your event... 

You can use markdown formatting:
- **bold text**
- *italic text*  
- ## Headings
- [links](https://example.com)
- Lists and more!"
                                    />
                                    <small className="form-text text-muted">
                                        Markdown is supported. Use **bold**, *italic*, ## headings, [links](url), and more!
                                    </small>
                                </div>
                            ) : (
                                <div className="border rounded p-3" style={{ minHeight: '200px', backgroundColor: '#f8f9fa' }}>
                                    {event.description.trim() ? (
                                        <ReactMarkdown>{event.description}</ReactMarkdown>
                                    ) : (
                                        <div className="text-muted fst-italic">
                                            Preview will appear here as you type in the Write tab...
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                        <div className="row">
                            <div className="col-md-6 mb-3">
                                <label className="form-label">Tags (comma-separated)</label>
                                <input type="text" className="form-control" name="tags" value={event.tags} onChange={handleChange} placeholder="networking, professional, workshop" />
                            </div>
                            <div className="col-md-6 mb-3">
                                <label className="form-label">Event Type</label>
                                <select className="form-select" name="eventType" value={event.eventType} onChange={handleChange}>
                                    <option value="IN_PERSON">In-Person Event</option>
                                    <option value="VIRTUAL">Virtual Event</option>
                                    <option value="HYBRID">Hybrid Event</option>
                                </select>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Date and Time */}
                <div className="card mb-4">
                    <div className="card-header">
                        <h3>Date and Time</h3>
                    </div>
                    <div className="card-body">
                        <div className="row">
                            <div className="col-md-4 mb-3">
                                <label className="form-label">Start Date & Time</label>
                                <input type="datetime-local" className="form-control" name="startDateTime" value={event.startDateTime} onChange={handleChange} />
                            </div>
                            <div className="col-md-4 mb-3">
                                <label className="form-label">End Date & Time</label>
                                <input type="datetime-local" className="form-control" name="endDateTime" value={event.endDateTime} onChange={handleChange} />
                            </div>
                            <div className="col-md-4 mb-3">
                                <label className="form-label">Timezone</label>
                                <select className="form-select" name="timezone" value={event.timezone} onChange={handleChange}>
                                    <option value="UTC">UTC</option>
                                    <option value="America/New_York">Eastern Time</option>
                                    <option value="America/Chicago">Central Time</option>
                                    <option value="America/Denver">Mountain Time</option>
                                    <option value="America/Los_Angeles">Pacific Time</option>
                                </select>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Location Information */}
                <div className="card mb-4">
                    <div className="card-header">
                        <h3>Location Information</h3>
                    </div>
                    <div className="card-body">
                        {event.eventType === 'IN_PERSON' || event.eventType === 'HYBRID' ? (
                            <>
                                <div className="row">
                                    <div className="col-md-6 mb-3">
                                        <label className="form-label">Venue Name</label>
                                        <input type="text" className="form-control" name="venueName" value={event.venueName} onChange={handleChange} placeholder="Conference Center, Hotel, etc." />
                                    </div>
                                    <div className="col-md-6 mb-3">
                                        <label className="form-label">Address</label>
                                        <input type="text" className="form-control" name="address" value={event.address} onChange={handleChange} placeholder="123 Main St" />
                                    </div>
                                </div>
                                <div className="row">
                                    <div className="col-md-3 mb-3">
                                        <label className="form-label">City</label>
                                        <input type="text" className="form-control" name="city" value={event.city} onChange={handleChange} />
                                    </div>
                                    <div className="col-md-3 mb-3">
                                        <label className="form-label">State</label>
                                        <input type="text" className="form-control" name="state" value={event.state} onChange={handleChange} />
                                    </div>
                                    <div className="col-md-3 mb-3">
                                        <label className="form-label">Zip Code</label>
                                        <input type="text" className="form-control" name="zipCode" value={event.zipCode} onChange={handleChange} />
                                    </div>
                                    <div className="col-md-3 mb-3">
                                        <label className="form-label">Country</label>
                                        <input type="text" className="form-control" name="country" value={event.country} onChange={handleChange} placeholder="United States" />
                                    </div>
                                </div>
                            </>
                        ) : null}

                        {event.eventType === 'VIRTUAL' || event.eventType === 'HYBRID' ? (
                            <>
                                <div className="row">
                                    <div className="col-md-6 mb-3">
                                        <label className="form-label">Virtual Meeting URL</label>
                                        <input type="url" className="form-control" name="virtualUrl" value={event.virtualUrl} onChange={handleChange} placeholder="https://zoom.us/j/..." />
                                    </div>
                                    <div className="col-md-3 mb-3">
                                        <label className="form-label">Dial-in Number</label>
                                        <input type="tel" className="form-control" name="dialInNumber" value={event.dialInNumber} onChange={handleChange} placeholder="+1-234-567-8900" />
                                    </div>
                                    <div className="col-md-3 mb-3">
                                        <label className="form-label">Access Code</label>
                                        <input type="text" className="form-control" name="accessCode" value={event.accessCode} onChange={handleChange} placeholder="123456" />
                                    </div>
                                </div>
                            </>
                        ) : null}
                    </div>
                </div>

                {/* Registration Settings */}
                <div className="card mb-4">
                    <div className="card-header">
                        <h3>Registration Settings</h3>
                    </div>
                    <div className="card-body">
                        <div className="row">
                            <div className="col-md-6 mb-3">
                                <label className="form-label">Maximum Registrations</label>
                                <input 
                                    type="number" 
                                    className="form-control" 
                                    name="maxRegistrations" 
                                    value={event.maxRegistrations} 
                                    onChange={handleChange}
                                    placeholder="Leave empty for unlimited"
                                    min="1"
                                />
                                <div className="form-text">Maximum number of people who can register (0 = unlimited)</div>
                            </div>
                            <div className="col-md-6 mb-3 d-flex align-items-end">
                                <div className="form-check">
                                    <input 
                                        type="checkbox" 
                                        className="form-check-input" 
                                        id="requiresApproval" 
                                        name="requiresApproval" 
                                        checked={event.requiresApproval} 
                                        onChange={handleChange} 
                                    />
                                    <label className="form-check-label" htmlFor="requiresApproval">
                                        Require approval for registration
                                    </label>
                                    <div className="form-text">Registrations will need to be approved by an organizer</div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Organizer Information */}
                <div className="card mb-4">
                    <div className="card-header">
                        <h3>Organizer Information</h3>
                    </div>
                    <div className="card-body">
                        <div className="row">
                            <div className="col-md-6 mb-3">
                                <label className="form-label">Organizer Name</label>
                                <input type="text" className="form-control" name="organizerName" value={event.organizerName} onChange={handleChange} />
                            </div>
                            <div className="col-md-6 mb-3">
                                <label className="form-label">Organizer Email</label>
                                <input type="email" className="form-control" name="organizerEmail" value={event.organizerEmail} onChange={handleChange} />
                            </div>
                        </div>
                        <div className="row">
                            <div className="col-md-6 mb-3">
                                <label className="form-label">Organizer Phone</label>
                                <input type="tel" className="form-control" name="organizerPhone" value={event.organizerPhone} onChange={handleChange} />
                            </div>
                            <div className="col-md-6 mb-3">
                                <label className="form-label">Organizer Website</label>
                                <input type="url" className="form-control" name="organizerWebsite" value={event.organizerWebsite} onChange={handleChange} />
                            </div>
                        </div>
                    </div>
                </div>

                {/* Event Images */}
                <div className="card mb-4">
                    <div className="card-header">
                        <h3>📸 Event Images</h3>
                        <p className="card-text mb-0">
                            <small className="text-muted">
                                Upload professional images to make your event stand out. 
                                Images will be automatically optimized and stored securely.
                            </small>
                        </p>
                    </div>
                    <div className="card-body">
                        <div className="row">
                            <div className="col-lg-8 mb-4">
                                <ImageUpload
                                    label="📊 Banner Image"
                                    type="banner"
                                    aspectRatio="16:9"
                                    currentImageUrl={event.bannerImageUrl}
                                    onImageUpload={(url) => setEvent(prevEvent => ({ ...prevEvent, bannerImageUrl: url }))}
                                    maxSize={10}
                                />
                                <div className="form-text mt-2">
                                    This image will be displayed as the main visual for your event on event listing pages and at the top of the event detail page.
                                </div>
                            </div>
                            <div className="col-lg-4 mb-4">
                                <ImageUpload
                                    label="🖼️ Thumbnail Image"
                                    type="thumbnail"
                                    aspectRatio="1:1"
                                    currentImageUrl={event.thumbnailImageUrl}
                                    onImageUpload={(url) => setEvent(prevEvent => ({ ...prevEvent, thumbnailImageUrl: url }))}
                                    maxSize={5}
                                />
                                <div className="form-text mt-2">
                                    Square image used in event cards and listings. Should be clear and recognizable at small sizes.
                                </div>
                            </div>
                        </div>
                        
                        {/* Image Guidelines */}
                        <div className="mt-4 p-3 bg-light rounded">
                            <h6 className="fw-bold mb-2">📋 Image Guidelines</h6>
                            <div className="row">
                                <div className="col-md-6">
                                    <h6 className="small fw-bold">Banner Image:</h6>
                                    <ul className="small text-muted mb-0">
                                        <li>Recommended: 1920x1080px (16:9)</li>
                                        <li>Maximum file size: 10MB</li>
                                        <li>Use high-quality, professional images</li>
                                        <li>Ensure text is readable if overlay text is used</li>
                                    </ul>
                                </div>
                                <div className="col-md-6">
                                    <h6 className="small fw-bold">Thumbnail Image:</h6>
                                    <ul className="small text-muted mb-0">
                                        <li>Recommended: 400x400px (1:1)</li>
                                        <li>Maximum file size: 5MB</li>
                                        <li>Should work well at small sizes</li>
                                        <li>Consider company logo or key visual</li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Event Details */}
                <div className="card mb-4">
                    <div className="card-header">
                        <h3>Additional Details</h3>
                    </div>
                    <div className="card-body">
                        <div className="row">
                            <div className="col-md-6 mb-3">
                                <label className="form-label">Capacity</label>
                                <input type="number" className="form-control" name="capacity" value={event.capacity} onChange={handleChange} />
                            </div>
                            <div className="col-md-6 mb-3 d-flex align-items-end">
                                <div className="form-check">
                                    <input type="checkbox" className="form-check-input" id="waitlistEnabled" name="waitlistEnabled" checked={event.waitlistEnabled} onChange={handleChange} />
                                    <label className="form-check-label" htmlFor="waitlistEnabled">Enable Waitlist</label>
                                </div>
                            </div>
                        </div>
                        <div className="mb-3">
                            <label className="form-label">Agenda/Schedule</label>
                            <textarea className="form-control" name="agenda" value={event.agenda} onChange={handleChange} rows={5} placeholder="Provide a detailed agenda or schedule for your event..."></textarea>
                        </div>
                    </div>
                </div>

                {/* Session Management */}
                <div className="card mb-4">
                    <div className="card-header">
                        <h3>📅 Session Management</h3>
                        <p className="card-text mb-0">
                            <small className="text-muted">
                                Configure whether this event has multiple sessions that attendees can register for individually.
                            </small>
                        </p>
                    </div>
                    <div className="card-body">
                        <div className="row mb-3">
                            <div className="col-md-6">
                                <div className="form-check">
                                    <input
                                        className="form-check-input"
                                        type="checkbox"
                                        id="isMultiSession"
                                        name="isMultiSession"
                                        checked={event.isMultiSession}
                                        onChange={handleChange}
                                    />
                                    <label className="form-check-label" htmlFor="isMultiSession">
                                        <strong>Multi-Session Event</strong>
                                    </label>
                                    <div className="form-text">
                                        Enable if this event has multiple sessions (workshops, breakouts, etc.)
                                    </div>
                                </div>
                            </div>
                            <div className="col-md-6">
                                <div className="form-check">
                                    <input
                                        className="form-check-input"
                                        type="checkbox"
                                        id="allowSessionSelection"
                                        name="allowSessionSelection"
                                        checked={event.allowSessionSelection}
                                        onChange={handleChange}
                                        disabled={!event.isMultiSession}
                                    />
                                    <label className="form-check-label" htmlFor="allowSessionSelection">
                                        <strong>Allow Session Selection</strong>
                                    </label>
                                    <div className="form-text">
                                        Let attendees choose which sessions to attend during registration
                                    </div>
                                </div>
                            </div>
                        </div>
                        
                        {event.isMultiSession && (
                            <div className="mt-4">
                                <SessionBuilder 
                                    eventId={id || 'new'} 
                                    onSessionsChange={(sessions) => {
                                        // Update isMultiSession based on number of sessions
                                        const hasMultipleSessions = sessions.length > 0;
                                        setEvent(prev => ({ 
                                            ...prev, 
                                            isMultiSession: hasMultipleSessions 
                                        }));
                                    }}
                                />
                            </div>
                        )}
                    </div>
                </div>

                {/* Registration Form Builder */}
                <div className="card mb-4">
                    <div className="card-header">
                        <h3>🎯 Registration Form Builder</h3>
                        <p className="card-text mb-0">
                            <small className="text-muted">
                                Build your custom registration form by adding fields below. 
                                Each event can collect different information from participants.
                            </small>
                        </p>
                    </div>
                    <div className="card-body">
                        <FormBuilder 
                            initialFields={(() => {
                                try {
                                    const parsed = event.formData ? JSON.parse(event.formData) : { fields: [] };
                                    return parsed.fields || [];
                                } catch {
                                    return [];
                                }
                            })()}
                            onChange={(fields) => {
                                const formData = JSON.stringify({ fields }, null, 2);
                                setEvent(prevEvent => ({ ...prevEvent, formData }));
                            }}
                        />
                        
                        {/* Advanced Options */}
                        <div className="mt-4 pt-4 border-top">
                            <h5>📋 Form Settings</h5>
                            <div className="row">
                                <div className="col-md-6">
                                    <div className="form-check">
                                        <input 
                                            className="form-check-input" 
                                            type="checkbox" 
                                            id="autoConfirm"
                                            defaultChecked
                                        />
                                        <label className="form-check-label" htmlFor="autoConfirm">
                                            Send automatic confirmation emails
                                        </label>
                                    </div>
                                </div>
                                <div className="col-md-6">
                                    <div className="form-check">
                                        <input 
                                            className="form-check-input" 
                                            type="checkbox" 
                                            id="allowEditRegistration"
                                            defaultChecked
                                        />
                                        <label className="form-check-label" htmlFor="allowEditRegistration">
                                            Allow participants to edit their registration
                                        </label>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* JSON Preview for Developers */}
                        <details className="mt-4">
                            <summary className="btn btn-link p-0">
                                <small>👨‍💻 View JSON (Developer Mode)</small>
                            </summary>
                            <div className="mt-2">
                                <textarea 
                                    className="form-control font-monospace small"
                                    rows={8}
                                    value={event.formData}
                                    onChange={handleChange}
                                    name="formData"
                                    placeholder="Form definition will appear here..."
                                />
                                <div className="form-text">
                                    Advanced users can directly edit the JSON form definition here.
                                </div>
                            </div>
                        </details>
                    </div>
                </div>

                <div className="d-flex gap-2 mb-5">
                    <button type="submit" className="btn btn-primary btn-lg">{id ? 'Update' : 'Create'} Event</button>
                    <button type="button" className="btn btn-secondary btn-lg" onClick={() => navigate('/admin')}>Cancel</button>
                </div>
            </form>
        </div>
    );
};

export default EventBuilder;