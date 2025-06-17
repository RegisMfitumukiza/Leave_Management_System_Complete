import React, { useEffect, useState } from 'react';
import { Box, Typography, CircularProgress, Alert, Paper, FormControl, InputLabel, Select, MenuItem } from '@mui/material';
import { Calendar, dateFnsLocalizer } from 'react-big-calendar';
import format from 'date-fns/format';
import parse from 'date-fns/parse';
import startOfWeek from 'date-fns/startOfWeek';
import getDay from 'date-fns/getDay';
import enUS from 'date-fns/locale/en-US';
import 'react-big-calendar/lib/css/react-big-calendar.css';
import { leaveApi } from '@/lib/api';
import dayjs from 'dayjs';

const locales = { 'en-US': enUS };
const localizer = dateFnsLocalizer({ format, parse, startOfWeek, getDay, locales });

const TeamCalendar = () => {
    const { user } = useAuth();
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [selectedMonth, setSelectedMonth] = useState(dayjs().format('YYYY-MM'));

    useEffect(() => {
        fetchTeamCalendar();
    }, [selectedMonth]);

    const fetchTeamCalendar = async () => {
        setLoading(true);
        setError('');
        try {
            // Use backend's unified team calendar endpoint
            const res = await leaveApi.get('/leaves/team-calendar', {
                params: { month: selectedMonth }
            });
            // Map backend leave responses to calendar events
            const calendarEvents = res.data.map(app => ({
                id: app.id,
                title: `${app.userFullName || app.user?.firstName + ' ' + app.user?.lastName || 'User'} (${app.leaveTypeName || app.leaveType?.name || ''})`,
                start: new Date(app.startDate),
                end: new Date(app.endDate),
                allDay: true,
            }));
            setEvents(calendarEvents);
        } catch (err) {
            setError('Failed to load team calendar');
            console.error('Team calendar fetch error:', err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <Box sx={{ p: 4 }}>
            <Typography variant="h4" sx={{ mb: 3 }}>Team Calendar</Typography>
            <Paper sx={{ p: 2 }}>
                {loading ? (
                    <Box display="flex" justifyContent="center" alignItems="center" minHeight="300px">
                        <CircularProgress />
                    </Box>
                ) : error ? (
                    <Alert severity="error">{error}</Alert>
                ) : (
                    <Calendar
                        localizer={localizer}
                        events={events}
                        startAccessor="start"
                        endAccessor="end"
                        style={{ height: 600 }}
                        eventPropGetter={() => ({ style: { backgroundColor: '#1976d2', color: 'white', borderRadius: '3px', opacity: 0.9 } })}
                        views={['month', 'week', 'day']}
                        defaultView="month"
                        tooltipAccessor={event => event.title}
                    />
                )}
            </Paper>
        </Box>
    );
};

export default TeamCalendar; 