import React, { useState, useEffect, ChangeEvent } from 'react';
import { useParams } from 'react-router-dom';
import apiClient from '../api/apiClient';

// Interface for attendee data
interface Attendee {
    registrationId: string;
    userName: string;
    userEmail: string;
    checkedIn: boolean;
}

const AttendancePage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const [attendees, setAttendees] = useState<Attendee[]>([]);
    const [searchTerm, setSearchTerm] = useState<string>('');

    const fetchAttendees = (name: string = ''): void => {
        apiClient.get(`/attendance/${id}`, { params: { name } })
            .then(response => {
                setAttendees(response.data);
            })
            .catch(error => console.error("Failed to fetch attendees", error));
    };

    useEffect(() => {
        const delayDebounceFn = setTimeout(() => {
            fetchAttendees(searchTerm);
        }, 300);

        return () => clearTimeout(delayDebounceFn);
    }, [id, searchTerm]);

    const handleCheckIn = (registrationId: string): void => {
        apiClient.put(`/attendance/${registrationId}/checkin`).then(() => {
            fetchAttendees(searchTerm); // Refresh the list with the current search term
        }).catch(error => console.error("Failed to check in attendee", error));
    };

    return (
        <div className="container">
            <h1 className="mt-5">Attendance</h1>
            <div className="mb-3">
                <input 
                    type="text"
                    className="form-control"
                    placeholder="Search by name..."
                    value={searchTerm}
                    onChange={(e: ChangeEvent<HTMLInputElement>) => setSearchTerm(e.target.value)}
                />
            </div>
            <table className="table table-striped">
                <thead className="table-dark">
                    <tr>
                        <th>Name</th>
                        <th>Email</th>
                        <th>Checked In</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {attendees.map(attendee => (
                        <tr key={attendee.registrationId}>
                            <td>{attendee.userName}</td>
                            <td>{attendee.userEmail}</td>
                            <td>
                                <span className={`badge ${attendee.checkedIn ? 'bg-success' : 'bg-secondary'}`}>
                                    {attendee.checkedIn ? 'Yes' : 'No'}
                                </span>
                            </td>
                            <td>
                                {!attendee.checkedIn && (
                                    <button className="btn btn-primary btn-sm" onClick={() => handleCheckIn(attendee.registrationId)}>Check In</button>
                                )}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

export default AttendancePage;