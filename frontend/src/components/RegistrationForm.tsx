import React, { useState, useEffect, FormEvent, ChangeEvent } from 'react';
import apiClient from '../api/apiClient';

interface FormField {
    name: string;
    type?: string;
    label: string;
    placeholder?: string;
    required?: boolean;
    rows?: number;
    options?: { value: string; label: string }[];
    checkboxLabel?: string;
    helpText?: string;
}

interface FormDefinition {
    fields: FormField[];
}

interface RegistrationFormProps {
    eventId: string;
    instanceId: string;
    onSuccess?: (registrationData: any) => void;
}

interface FormData {
    [key: string]: string | boolean;
}

const RegistrationForm: React.FC<RegistrationFormProps> = ({ eventId, instanceId, onSuccess }) => {
    const [formDefinition, setFormDefinition] = useState<FormDefinition | null>(null);
    const [formData, setFormData] = useState<FormData>({});
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState(false);

    useEffect(() => {
        if (eventId) {
            apiClient.get(`/events/${eventId}/form`).then(response => {
                // Ensure the response data is not empty and is valid JSON
                if (response.data && typeof response.data === 'string') {
                    try {
                        const parsedData = JSON.parse(response.data);
                        setFormDefinition(parsedData);
                    } catch (e) {
                        console.error("Failed to parse form definition JSON", e);
                        setError('Failed to load registration form: Invalid format.');
                    }
                } else if (response.data) {
                    setFormDefinition(response.data); // Already an object
                } else {
                    setFormDefinition({ fields: [] }); // No form defined
                }
            }).catch(err => {
                console.error("Failed to load form definition", err);
                setError('Failed to load registration form.');
            });
        }
    }, [eventId]);

    const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
        const target = e.target as HTMLInputElement;
        const { name, value, type } = target;
        const checked = target.checked;
        setFormData(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setIsSubmitting(true);
        setError(null);
        setSuccess(false);

        const submissionData = {
            eventInstanceId: instanceId,
            userEmail: formData.email, // Assuming 'email' is a standard field
            userName: formData.name, // Assuming 'name' is a standard field
            formData: JSON.stringify(formData)
        };

        apiClient.post('/registrations', submissionData)
            .then(response => {
                setSuccess(true);
                setFormData({});
                if (onSuccess) {
                    onSuccess({
                        id: response.data.id,
                        userName: submissionData.userName,
                        userEmail: submissionData.userEmail
                    });
                }
            })
            .catch(err => {
                setError('Registration failed. Please try again.');
                console.error('Registration error', err);
            })
            .finally(() => {
                setIsSubmitting(false);
            });
    };

    if (error) {
        return <div className="alert alert-danger">{error}</div>;
    }

    if (!formDefinition || !formDefinition.fields) {
        return <div>Loading form...</div>;
    }

    const renderField = (field: FormField) => {
        switch(field.type) {
            case 'textarea':
                return (
                    <textarea
                        className="form-control"
                        id={field.name}
                        name={field.name}
                        placeholder={field.placeholder}
                        required={field.required}
                        value={typeof formData[field.name] === 'string' ? formData[field.name] as string : ''}
                        onChange={handleChange}
                        rows={field.rows || 3}
                    />
                );
            case 'select':
                return (
                    <select
                        className="form-select"
                        id={field.name}
                        name={field.name}
                        required={field.required}
                        value={typeof formData[field.name] === 'string' ? formData[field.name] as string : ''}
                        onChange={handleChange}
                    >
                        <option value="">{field.placeholder || 'Select an option'}</option>
                        {field.options && field.options.map(option => (
                            <option key={option.value} value={option.value}>
                                {option.label}
                            </option>
                        ))}
                    </select>
                );
            case 'radio':
                return (
                    <div>
                        {field.options && field.options.map(option => (
                            <div className="form-check" key={option.value}>
                                <input
                                    className="form-check-input"
                                    type="radio"
                                    name={field.name}
                                    id={`${field.name}-${option.value}`}
                                    value={option.value}
                                    checked={formData[field.name] === option.value}
                                    onChange={handleChange}
                                    required={field.required}
                                />
                                <label className="form-check-label" htmlFor={`${field.name}-${option.value}`}>
                                    {option.label}
                                </label>
                            </div>
                        ))}
                    </div>
                );
            case 'checkbox':
                return (
                    <div className="form-check">
                        <input
                            className="form-check-input"
                            type="checkbox"
                            id={field.name}
                            name={field.name}
                            checked={!!formData[field.name]}
                            onChange={handleChange}
                            required={field.required}
                        />
                        <label className="form-check-label" htmlFor={field.name}>
                            {field.checkboxLabel || field.label}
                        </label>
                    </div>
                );
            default:
                return (
                    <input
                        type={field.type || 'text'}
                        className="form-control"
                        id={field.name}
                        name={field.name}
                        placeholder={field.placeholder}
                        required={field.required}
                        value={typeof formData[field.name] === 'string' ? formData[field.name] as string : ''}
                        onChange={handleChange}
                    />
                );
        }
    };

    return (
        <form onSubmit={handleSubmit}>
            {formDefinition.fields.map(field => (
                <div className="mb-3" key={field.name}>
                    {field.type !== 'checkbox' && (
                        <label htmlFor={field.name} className="form-label">{field.label}</label>
                    )}
                    {renderField(field)}
                    {field.helpText && (
                        <div className="form-text">{field.helpText}</div>
                    )}
                </div>
            ))}
            <div className="mb-3 form-check">
                <input type="checkbox" className="form-check-input" id="notification-ack" required />
                <label className="form-check-label" htmlFor="notification-ack">I acknowledge that I will receive notifications about this event.</label>
            </div>
            <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
                {isSubmitting ? 'Registering...' : 'Register'}
            </button>
            {success && <div className="alert alert-success mt-3">Registration successful!</div>}
        </form>
    );
};

export default RegistrationForm;
