import React, { useState, useCallback } from 'react';

interface FormField {
    id: string;
    type: string;
    label: string;
    required: boolean;
    placeholder?: string;
    helpText?: string;
    options?: Array<{ value: string; label: string }>;
    rows?: number;
    validation?: {
        minLength?: number;
        maxLength?: number;
        pattern?: string;
    };
}

interface FormBuilderProps {
    initialFields: FormField[];
    onChange: (fields: FormField[]) => void;
}

const FormBuilder: React.FC<FormBuilderProps> = ({ initialFields, onChange }) => {
    const [fields, setFields] = useState<FormField[]>(initialFields);
    const [draggedIndex, setDraggedIndex] = useState<number | null>(null);

    const fieldTypes = [
        { value: 'text', label: 'Text Input', icon: '📝' },
        { value: 'email', label: 'Email', icon: '📧' },
        { value: 'phone', label: 'Phone Number', icon: '📞' },
        { value: 'number', label: 'Number', icon: '#️⃣' },
        { value: 'date', label: 'Date', icon: '📅' },
        { value: 'textarea', label: 'Multi-line Text', icon: '📄' },
        { value: 'select', label: 'Dropdown', icon: '📋' },
        { value: 'radio', label: 'Multiple Choice', icon: '🔘' },
        { value: 'checkbox', label: 'Checkboxes', icon: '☑️' },
        { value: 'file', label: 'File Upload', icon: '📎' },
    ];

    const generateId = () => `field_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

    const addField = useCallback((type: string) => {
        const newField: FormField = {
            id: generateId(),
            type,
            label: `New ${fieldTypes.find(ft => ft.value === type)?.label || type}`,
            required: false,
            placeholder: '',
            helpText: '',
        };

        if (type === 'select' || type === 'radio' || type === 'checkbox') {
            newField.options = [
                { value: 'option1', label: 'Option 1' },
                { value: 'option2', label: 'Option 2' },
            ];
        }

        if (type === 'textarea') {
            newField.rows = 3;
        }

        const updatedFields = [...fields, newField];
        setFields(updatedFields);
        onChange(updatedFields);
    }, [fields, onChange]);

    const updateField = useCallback((index: number, updates: Partial<FormField>) => {
        const updatedFields = fields.map((field, i) => 
            i === index ? { ...field, ...updates } : field
        );
        setFields(updatedFields);
        onChange(updatedFields);
    }, [fields, onChange]);

    const removeField = useCallback((index: number) => {
        const updatedFields = fields.filter((_, i) => i !== index);
        setFields(updatedFields);
        onChange(updatedFields);
    }, [fields, onChange]);

    const moveField = useCallback((fromIndex: number, toIndex: number) => {
        const updatedFields = [...fields];
        const [movedField] = updatedFields.splice(fromIndex, 1);
        updatedFields.splice(toIndex, 0, movedField);
        setFields(updatedFields);
        onChange(updatedFields);
    }, [fields, onChange]);

    const handleDragStart = (e: React.DragEvent, index: number) => {
        setDraggedIndex(index);
        e.dataTransfer.effectAllowed = 'move';
    };

    const handleDragOver = (e: React.DragEvent) => {
        e.preventDefault();
        e.dataTransfer.dropEffect = 'move';
    };

    const handleDrop = (e: React.DragEvent, dropIndex: number) => {
        e.preventDefault();
        if (draggedIndex !== null && draggedIndex !== dropIndex) {
            moveField(draggedIndex, dropIndex);
        }
        setDraggedIndex(null);
    };

    const addOption = (fieldIndex: number) => {
        const field = fields[fieldIndex];
        if (field.options) {
            const newOption = { 
                value: `option${field.options.length + 1}`, 
                label: `Option ${field.options.length + 1}` 
            };
            updateField(fieldIndex, {
                options: [...field.options, newOption]
            });
        }
    };

    const updateOption = (fieldIndex: number, optionIndex: number, value: string, label: string) => {
        const field = fields[fieldIndex];
        if (field.options) {
            const updatedOptions = field.options.map((option, i) => 
                i === optionIndex ? { value, label } : option
            );
            updateField(fieldIndex, { options: updatedOptions });
        }
    };

    const removeOption = (fieldIndex: number, optionIndex: number) => {
        const field = fields[fieldIndex];
        if (field.options && field.options.length > 1) {
            const updatedOptions = field.options.filter((_, i) => i !== optionIndex);
            updateField(fieldIndex, { options: updatedOptions });
        }
    };

    return (
        <div className="form-builder">
            {/* Field Type Palette */}
            <div className="card mb-4">
                <div className="card-header">
                    <h5 className="card-title mb-0">📋 Add Form Fields</h5>
                    <small className="text-muted">Click to add a field to your registration form</small>
                </div>
                <div className="card-body">
                    <div className="row g-2">
                        {fieldTypes.map((fieldType) => (
                            <div key={fieldType.value} className="col-md-3 col-sm-4 col-6">
                                <button
                                    type="button"
                                    className="btn btn-outline-primary w-100 h-100 p-3"
                                    onClick={() => addField(fieldType.value)}
                                >
                                    <div className="d-flex flex-column align-items-center">
                                        <span style={{ fontSize: '1.5rem' }}>{fieldType.icon}</span>
                                        <small className="mt-1">{fieldType.label}</small>
                                    </div>
                                </button>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            {/* Form Fields List */}
            <div className="card">
                <div className="card-header d-flex justify-content-between align-items-center">
                    <h5 className="card-title mb-0">🏗️ Form Fields ({fields.length})</h5>
                    {fields.length === 0 && (
                        <small className="text-muted">Add fields above to get started</small>
                    )}
                </div>
                <div className="card-body">
                    {fields.length === 0 ? (
                        <div className="text-center py-5">
                            <div className="text-muted">
                                <span style={{ fontSize: '3rem' }}>📝</span>
                                <h6 className="mt-2">No fields added yet</h6>
                                <p>Click on field types above to start building your form</p>
                            </div>
                        </div>
                    ) : (
                        <div className="form-fields-list">
                            {fields.map((field, index) => (
                                <div
                                    key={field.id}
                                    className={`card mb-3 field-card ${draggedIndex === index ? 'dragging' : ''}`}
                                    draggable
                                    onDragStart={(e) => handleDragStart(e, index)}
                                    onDragOver={handleDragOver}
                                    onDrop={(e) => handleDrop(e, index)}
                                >
                                    <div className="card-body">
                                        <div className="d-flex justify-content-between align-items-start mb-3">
                                            <div className="d-flex align-items-center">
                                                <span className="drag-handle me-2" style={{ cursor: 'move' }}>⋮⋮</span>
                                                <span className="me-2">
                                                    {fieldTypes.find(ft => ft.value === field.type)?.icon}
                                                </span>
                                                <strong>{field.label}</strong>
                                                {field.required && <span className="badge bg-danger ms-2">Required</span>}
                                            </div>
                                            <button
                                                type="button"
                                                className="btn btn-sm btn-outline-danger"
                                                onClick={() => removeField(index)}
                                                title="Remove field"
                                            >
                                                🗑️
                                            </button>
                                        </div>

                                        <div className="row g-3">
                                            <div className="col-md-6">
                                                <label className="form-label">Field Label</label>
                                                <input
                                                    type="text"
                                                    className="form-control"
                                                    value={field.label}
                                                    onChange={(e) => updateField(index, { label: e.target.value })}
                                                />
                                            </div>
                                            <div className="col-md-6">
                                                <label className="form-label">Placeholder Text</label>
                                                <input
                                                    type="text"
                                                    className="form-control"
                                                    value={field.placeholder || ''}
                                                    onChange={(e) => updateField(index, { placeholder: e.target.value })}
                                                />
                                            </div>
                                            <div className="col-md-8">
                                                <label className="form-label">Help Text</label>
                                                <input
                                                    type="text"
                                                    className="form-control"
                                                    value={field.helpText || ''}
                                                    onChange={(e) => updateField(index, { helpText: e.target.value })}
                                                    placeholder="Optional text to help users fill this field"
                                                />
                                            </div>
                                            <div className="col-md-4 d-flex align-items-end">
                                                <div className="form-check">
                                                    <input
                                                        className="form-check-input"
                                                        type="checkbox"
                                                        checked={field.required}
                                                        onChange={(e) => updateField(index, { required: e.target.checked })}
                                                        id={`required-${index}`}
                                                    />
                                                    <label className="form-check-label" htmlFor={`required-${index}`}>
                                                        Required Field
                                                    </label>
                                                </div>
                                            </div>

                                            {/* Additional options for specific field types */}
                                            {field.type === 'textarea' && (
                                                <div className="col-md-4">
                                                    <label className="form-label">Number of Rows</label>
                                                    <input
                                                        type="number"
                                                        className="form-control"
                                                        value={field.rows || 3}
                                                        min="2"
                                                        max="10"
                                                        onChange={(e) => updateField(index, { rows: parseInt(e.target.value) })}
                                                    />
                                                </div>
                                            )}

                                            {/* Options for select, radio, checkbox */}
                                            {(field.type === 'select' || field.type === 'radio' || field.type === 'checkbox') && (
                                                <div className="col-12">
                                                    <label className="form-label">Options</label>
                                                    <div className="options-list">
                                                        {field.options?.map((option, optionIndex) => (
                                                            <div key={optionIndex} className="input-group mb-2">
                                                                <input
                                                                    type="text"
                                                                    className="form-control"
                                                                    placeholder="Option label"
                                                                    value={option.label}
                                                                    onChange={(e) => updateOption(index, optionIndex, option.value, e.target.value)}
                                                                />
                                                                <input
                                                                    type="text"
                                                                    className="form-control"
                                                                    placeholder="Option value"
                                                                    value={option.value}
                                                                    onChange={(e) => updateOption(index, optionIndex, e.target.value, option.label)}
                                                                />
                                                                {field.options && field.options.length > 1 && (
                                                                    <button
                                                                        className="btn btn-outline-danger"
                                                                        type="button"
                                                                        onClick={() => removeOption(index, optionIndex)}
                                                                    >
                                                                        ✕
                                                                    </button>
                                                                )}
                                                            </div>
                                                        ))}
                                                        <button
                                                            type="button"
                                                            className="btn btn-sm btn-outline-success"
                                                            onClick={() => addOption(index)}
                                                        >
                                                            + Add Option
                                                        </button>
                                                    </div>
                                                </div>
                                            )}
                                        </div>

                                        {/* Field Preview */}
                                        <div className="mt-3 p-3 bg-light rounded">
                                            <small className="text-muted d-block mb-2">Preview:</small>
                                            <FormFieldPreview field={field} />
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>

            <style>{`
                .field-card {
                    transition: all 0.2s ease;
                    border: 1px solid #dee2e6;
                }
                .field-card:hover {
                    border-color: #007bff;
                    box-shadow: 0 0.125rem 0.25rem rgba(0, 123, 255, 0.075);
                }
                .field-card.dragging {
                    opacity: 0.5;
                    transform: rotate(2deg);
                }
                .drag-handle {
                    color: #6c757d;
                    font-size: 1.2rem;
                }
                .drag-handle:hover {
                    color: #007bff;
                }
                .options-list .input-group input:first-child {
                    border-top-right-radius: 0;
                    border-bottom-right-radius: 0;
                }
                .options-list .input-group input:nth-child(2) {
                    border-radius: 0;
                    border-left: 0;
                    border-right: 0;
                }
            `}</style>
        </div>
    );
};

const FormFieldPreview: React.FC<{ field: FormField }> = ({ field }) => {
    const renderField = () => {
        switch (field.type) {
            case 'textarea':
                return (
                    <textarea
                        className="form-control"
                        placeholder={field.placeholder}
                        rows={field.rows || 3}
                        disabled
                    />
                );
            case 'select':
                return (
                    <select className="form-select" disabled>
                        <option value="">Choose...</option>
                        {field.options?.map((option, i) => (
                            <option key={i} value={option.value}>{option.label}</option>
                        ))}
                    </select>
                );
            case 'radio':
                return (
                    <div>
                        {field.options?.map((option, i) => (
                            <div key={i} className="form-check">
                                <input
                                    className="form-check-input"
                                    type="radio"
                                    name={`preview-${field.id}`}
                                    disabled
                                />
                                <label className="form-check-label">{option.label}</label>
                            </div>
                        ))}
                    </div>
                );
            case 'checkbox':
                return (
                    <div>
                        {field.options?.map((option, i) => (
                            <div key={i} className="form-check">
                                <input
                                    className="form-check-input"
                                    type="checkbox"
                                    disabled
                                />
                                <label className="form-check-label">{option.label}</label>
                            </div>
                        ))}
                    </div>
                );
            case 'file':
                return <input type="file" className="form-control" disabled />;
            default:
                return (
                    <input
                        type={field.type}
                        className="form-control"
                        placeholder={field.placeholder}
                        disabled
                    />
                );
        }
    };

    return (
        <div>
            <label className="form-label">
                {field.label}
                {field.required && <span className="text-danger"> *</span>}
            </label>
            {renderField()}
            {field.helpText && (
                <div className="form-text">{field.helpText}</div>
            )}
        </div>
    );
};

export default FormBuilder;