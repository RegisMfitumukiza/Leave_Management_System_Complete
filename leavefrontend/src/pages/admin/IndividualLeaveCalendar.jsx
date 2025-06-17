import React, { useState, useEffect } from 'react';
import { Box, Typography, Paper, Grid, FormControl, InputLabel, Select, MenuItem, CircularProgress, Alert } from '@mui/material';
import { Calendar, dateFnsLocalizer } from 'react-big-calendar';
import format from 'date-fns/format';
import parse from 'date-fns/parse';
import startOfWeek from 'date-fns/startOfWeek';
import getDay from 'date-fns/getDay';
import enUS from 'date-fns/locale/en-US';
import { leaveApi, authApi } from '@/lib/api';
import 'react-big-calendar/lib/css/react-big-calendar.css';

const locales = {
    'en-US': enUS
};

const localizer = dateFnsLocalizer({
    format,
    parse,
    startOfWeek,
    getDay,
    locales,
});

const testEvents = [
    {
        id: 1,
        title: "Test Event",
        start: new Date(),
        end: new Date(new Date().getTime() + 24 * 60 * 60 * 1000),
        status: "APPROVED",
        allDay: true,
    }
];

const IndividualLeaveCalendar = () => {
    const [users, setUsers] = useState([]);
    const [selectedUser, setSelectedUser] = useState('');
    const [leaveEvents, setLeaveEvents] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        // Fetch users for the dropdown
        const fetchUsers = async () => {
            try {
                const response = await authApi.get('/users');
                setUsers(response.data);
            } catch (err) {
                setError('Failed to load users');
            }
        };
        fetchUsers();
    }, []);

    useEffect(() => {
        if (selectedUser) {
            fetchUserLeaveEvents();
        }
    }, [selectedUser]);

    const fetchUserLeaveEvents = async () => {
        setLoading(true);
        setError('');
        try {
            const response = await leaveApi.get(`/leaves?userId=${selectedUser}`);
            const events = response.data.map(application => ({
                id: application.id,
                title: `${application.leaveType || application.leaveTypeName || ''} - ${application.employeeName || ''}`,
                start: new Date(application.startDate),
                end: new Date(application.endDate),
                status: application.status,
                allDay: true,
                resource: application
            }));
            setLeaveEvents(events);
        } catch (err) {
            setError('Failed to load leave events');
        } finally {
            setLoading(false);
        }
    };

    const eventStyleGetter = (event) => {
        let backgroundColor = '#3174ad'; // default color
        switch (event.status) {
            case 'APPROVED':
                backgroundColor = '#4caf50';
                break;
            case 'REJECTED':
                backgroundColor = '#f44336';
                break;
            case 'PENDING':
                backgroundColor = '#ff9800';
                break;
            default:
                break;
        }

        return {
            style: {
                backgroundColor,
                borderRadius: '3px',
                opacity: 0.8,
                color: 'white',
                border: '0px',
                display: 'block'
            }
        };
    };

    return (
        <Box sx={{ p: 3 }}>
            <Typography variant="h6" sx={{ mb: 3 }}>Individual Leave Calendar</Typography>
            <Grid container spacing={3}>
                <Grid item xs={12} md={4}>
                    <FormControl fullWidth>
                        <InputLabel>Select Employee</InputLabel>
                        <Select
                            value={selectedUser}
                            label="Select Employee"
                            onChange={(e) => setSelectedUser(e.target.value)}
                        >
                            {users.map((user) => (
                                <MenuItem key={user.id} value={user.id}>
                                    {user.firstName} {user.lastName} ({user.email})
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                </Grid>
            </Grid>
            {error && (
                <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>
            )}
            {loading ? (
                <Box display="flex" justifyContent="center" p={3}>
                    <CircularProgress />
                </Box>
            ) : (
                <Paper sx={{ mt: 3, p: 2 }}>
                    <Calendar
                        localizer={localizer}
                        events={leaveEvents}
                        startAccessor="start"
                        endAccessor="end"
                        style={{ height: 600 }}
                        eventPropGetter={eventStyleGetter}
                        views={['month', 'week', 'day']}
                        defaultView="month"
                        defaultDate={leaveEvents.length > 0 ? leaveEvents[0].start : new Date()}
                        tooltipAccessor={(event) => `${event.title} (${event.status})`}
                    />
                </Paper>
            )}
            <Box sx={{ mt: 2, display: 'flex', gap: 2, alignItems: 'center' }}>
                <Typography variant="subtitle2">Legend:</Typography>
                <Box sx={{ display: 'flex', gap: 2 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Box sx={{ width: 20, height: 20, bgcolor: '#4caf50', borderRadius: 1 }} />
                        <Typography variant="body2">Approved</Typography>
                    </Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Box sx={{ width: 20, height: 20, bgcolor: '#f44336', borderRadius: 1 }} />
                        <Typography variant="body2">Rejected</Typography>
                    </Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Box sx={{ width: 20, height: 20, bgcolor: '#ff9800', borderRadius: 1 }} />
                        <Typography variant="body2">Pending</Typography>
                    </Box>
                </Box>
            </Box>
        </Box>
    );
};

export default IndividualLeaveCalendar; 