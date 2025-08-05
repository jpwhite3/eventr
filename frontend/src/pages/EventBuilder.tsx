import React, { useState, useEffect, ChangeEvent, FormEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import apiClient from '../api/apiClient';

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
}

// Interface for API event data (when tags is an array)
interface ApiEventData extends Omit<EventData, 'tags'> {
    tags?: string[];
}

const EventBuilder: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const [event, setEvent] = useState<EventData>({
        name: '',
        description: '',
        tags: '',
        capacity: 0,
        waitlistEnabled: false,
        bannerImageUrl: '',
        thumbnailImageUrl: '',
        formData: ''
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

    const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>): void => {
        const target = e.target as HTMLInputElement;
        const { name, value, type } = target;
        const checked = target.checked;
        setEvent(prevEvent => ({
            ...prevEvent,
            [name]: type === 'checkbox' ? checked : value
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
                <div className="row">
                    <div className="col-md-6 mb-3">
                        <label className="form-label">Event Name</label>
                        <input type="text" className="form-control" name="name" value={event.name} onChange={handleChange} required />
                    </div>
                    <div className="col-md-6 mb-3">
                        <label className="form-label">Tags (comma-separated)</label>
                        <input type="text" className="form-control" name="tags" value={event.tags} onChange={handleChange} />
                    </div>
                </div>
                <div className="mb-3">
                    <label className="form-label">Description (Markdown supported)</label>
                    <textarea className="form-control" name="description" value={event.description} onChange={handleChange} rows={5}></textarea>
                </div>
                <div className="row">
                    <div className="col-md-6 mb-3">
                        <label className="form-label">Banner Image URL</label>
                        <input type="text" className="form-control" name="bannerImageUrl" value={event.bannerImageUrl} onChange={handleChange} />
                    </div>
                    <div className="col-md-6 mb-3">
                        <label className="form-label">Thumbnail Image URL</label>
                        <input type="text" className="form-control" name="thumbnailImageUrl" value={event.thumbnailImageUrl} onChange={handleChange} />
                    </div>
                </div>
                <div className="row align-items-end">
                    <div className="col-md-6 mb-3">
                        <label className="form-label">Capacity</label>
                        <input type="number" className="form-control" name="capacity" value={event.capacity} onChange={handleChange} />
                    </div>
                    <div className="col-md-6 mb-3">
                        <div className="form-check">
                            <input type="checkbox" className="form-check-input" id="waitlistEnabled" name="waitlistEnabled" checked={event.waitlistEnabled} onChange={handleChange} />
                            <label className="form-check-label" htmlFor="waitlistEnabled">Enable Waitlist</label>
                        </div>
                    </div>
                </div>
                
                <hr className="my-4" />

                <h2 className="mb-3">Registration Form Definition</h2>
                <div className="mb-3">
                    <label htmlFor="formData" className="form-label">Form Fields (JSON format)</label>
                    <textarea 
                        className="form-control"
                        id="formData"
                        name="formData"
                        rows={10}
                        value={event.formData}
                        onChange={handleChange}
                        placeholder='e.g., { "fields": [ { "name": "fullName", "label": "Full Name", "type": "text", "required": true } ] }'
                    ></textarea>
                    <div className="form-text">
                        <p>Define your form fields as a JSON object with the following structure:</p>
                        <pre className="text-muted bg-light p-2">
{`{
  "fields": [
    {
      "name": "fullName",     // Field identifier (required)
      "label": "Full Name",   // Display label (required)
      "type": "text",        // Field type (required)
      "required": true,      // Whether field is required (optional)
      "placeholder": "John Doe", // Placeholder text (optional)
      "helpText": "Enter your legal name" // Help text (optional)
    }
  ]
}`}
                        </pre>
                        <p>Supported field types:</p>
                        <ul className="text-muted small">
                            <li><code>text</code>, <code>email</code>, <code>tel</code>, <code>number</code>, <code>date</code> - Standard input fields</li>
                            <li><code>textarea</code> - Multiline text entry (add <code>rows</code> property to control height)</li>
                            <li><code>select</code> - Dropdown list (requires <code>options</code> array with <code>value</code> and <code>label</code> properties)</li>
                            <li><code>radio</code> - Radio button group (requires <code>options</code> array)</li>
                            <li><code>checkbox</code> - Single checkbox (use <code>checkboxLabel</code> for the text next to checkbox)</li>
                        </ul>
                    </div>
                </div>

                <button type="submit" className="btn btn-primary mt-4">{id ? 'Update' : 'Create'} Event</button>
            </form>
        </div>
    );
};

export default EventBuilder;