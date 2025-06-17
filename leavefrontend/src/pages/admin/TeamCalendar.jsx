import React, { useState, useEffect, useRef } from 'react';
import { Box, Typography, Select, MenuItem, FormControl, InputLabel, CircularProgress, Alert, Button, Paper, TextField } from '@mui/material';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import { authApi } from '@/lib/api';
import { DatePicker, LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { leaveApi } from '@/lib/api';

const TeamCalendar = () => {
    const [currentDate, setCurrentDate] = useState(new Date());
    const [dateRange, setDateRange] = useState([null, null]);
    const [events, setEvents] = useState([]);
    const [departments, setDepartments] = useState([]);
    const [selectedDepartment, setSelectedDepartment] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const calendarRef = useRef(null);
    const [autoRefresh, setAutoRefresh] = useState(false);
    const autoRefreshInterval = 60000; // 60 seconds (adjust as needed)

    useEffect(() => {
        fetchDepartments();
    }, []);

    useEffect(() => {
        if (dateRange[0] && dateRange[1]) {
            fetchEvents(dateRange[0], dateRange[1]);
        } else {
            // Fallback: if no range is selected, fetch events for the current month (using currentDate).
            const startDate = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1);
            const endDate = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 0);
            fetchEvents(startDate, endDate);
        }
        if (autoRefresh) {
            const interval = setInterval(() => {
                if (dateRange[0] && dateRange[1]) {
                    fetchEvents(dateRange[0], dateRange[1]);
                } else {
                    const startDate = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1);
                    const endDate = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 0);
                    fetchEvents(startDate, endDate);
                }
            }, autoRefreshInterval);
            return () => clearInterval(interval);
        }
    }, [dateRange, currentDate, selectedDepartment, autoRefresh]);

    useEffect(() => {
        if (dateRange[0] && calendarRef.current) {
            queueMicrotask(() => {
                calendarRef.current.getApi().gotoDate(dateRange[0]);
            });
        }
    }, [dateRange]);

    const fetchDepartments = async () => {
        try {
            const res = await authApi.get('/departments');
            setDepartments(res.data);
        } catch (err) {
            console.error('Error fetching departments:', err);
            setError('Failed to load departments.');
        }
    };

    const fetchEvents = async (start, end) => {
        setLoading(true);
        setError(null);
        let startDate, endDate;
        if (start && end) {
            startDate = start instanceof Date ? start.toISOString().split('T')[0] : start;
            endDate = end instanceof Date ? end.toISOString().split('T')[0] : end;
        } else {
            startDate = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1).toISOString().split('T')[0];
            endDate = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 0).toISOString().split('T')[0];
        }
        const url = selectedDepartment
            ? `/leaves/team-calendar/${selectedDepartment}?startDate=${startDate}&endDate=${endDate}`
            : `/leaves/team-calendar?startDate=${startDate}&endDate=${endDate}`;
        try {
            const res = await leaveApi.get(url);
            const fullCalendarEvents = res.data.map(event => ({
                id: event.id,
                title: `${event.employeeName || 'Unknown'} (${event.leaveType || event.leaveTypeName || ''})`,
                start: event.startDate,
                end: event.endDate,
                color: event.color || (event.status === 'APPROVED' ? '#1976d2' : '#bdbdbd'),
                extendedProps: {
                    type: event.type,
                    userName: event.employeeName,
                    leaveType: event.leaveType || event.leaveTypeName,
                    status: event.status
                }
            }));
            setEvents(fullCalendarEvents);
        } catch (err) {
            console.error('Error fetching calendar events:', err);
            setError('Failed to load calendar events.');
        } finally {
            setLoading(false);
        }
    };

    const renderEventTooltip = (event) => {
        const { type, userName, leaveType, status } = event.extendedProps;
        if (type === 'HOLIDAY') {
            return `${event.title} (Holiday)`;
        } else {
            return `${userName} (${leaveType}) – ${status}`;
        }
    };

    const renderEvents = () => {
        if (loading) return <CircularProgress />;
        if (error) return <Alert severity="error">{error}</Alert>;
        if (events.length === 0) return <Alert severity="info">No events found for the selected range.</Alert>;
        return null; // FullCalendar renders events automatically.
    };

    const handleDateChange = (newValue, isStartDate = true) => {
        if (isStartDate) {
            setDateRange([newValue, dateRange[1]]);
            if (newValue && calendarRef.current) {
                queueMicrotask(() => {
                    calendarRef.current.getApi().gotoDate(newValue);
                });
            }
        } else {
            setDateRange([dateRange[0], newValue]);
            if (newValue && !dateRange[0] && calendarRef.current) {
                queueMicrotask(() => {
                    calendarRef.current.getApi().gotoDate(newValue);
                });
            }
        }
    };

    return (
        <Box sx={{ p: 2 }}>
            <Typography variant="h4" gutterBottom> Team Calendar </Typography>
            <FormControl sx={{ minWidth: 200, mb: 2 }}>
                <InputLabel id="department-select-label">Filter by Department</InputLabel>
                <Select
                    labelId="department-select-label"
                    value={selectedDepartment}
                    label="Filter by Department"
                    onChange={(e) => setSelectedDepartment(e.target.value)}
                >
                    <MenuItem value=""> All Departments </MenuItem>
                    {departments.map((dept) => (<MenuItem key={dept.id} value={dept.id}> {dept.name} </MenuItem>))}
                </Select>
            </FormControl>
            <LocalizationProvider dateAdapter={AdapterDateFns}>
                <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
                    <DatePicker
                        label="Start date"
                        value={dateRange[0]}
                        onChange={(newValue) => handleDateChange(newValue, true)}
                        slotProps={{ textField: { fullWidth: true } }}
                    />
                    <DatePicker
                        label="End date"
                        value={dateRange[1]}
                        onChange={(newValue) => handleDateChange(newValue, false)}
                        slotProps={{ textField: { fullWidth: true } }}
                    />
                </Box>
            </LocalizationProvider>
            <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 1, mt: 2 }}>
                <Button variant="outlined" onClick={() => {
                    if (dateRange[0] && dateRange[1]) fetchEvents(dateRange[0], dateRange[1]);
                }} disabled={loading}> Refresh </Button>
                <Button variant="outlined" onClick={() => setAutoRefresh(!autoRefresh)} sx={{ ml: 1 }}> {autoRefresh ? "Disable Auto Refresh" : "Enable Auto Refresh"} </Button>
            </Box>
            <Paper elevation={2} sx={{ p: 1, mb: 2 }}>
                <Typography variant="subtitle1" gutterBottom>
                    Selected Range: {dateRange[0] && dateRange[1] ? `${dateRange[0].toLocaleDateString()} – ${dateRange[1].toLocaleDateString()}` : "None"}
                </Typography>
            </Paper>
            <FullCalendar
                ref={calendarRef}
                plugins={[dayGridPlugin, interactionPlugin]}
                initialView="dayGridMonth"
                events={events}
                eventContent={(eventInfo) => {
                    const { title, extendedProps } = eventInfo.event;
                    const tooltip = renderEventTooltip(eventInfo.event);
                    return <Box title={tooltip} sx={{ p: 0.5, borderRadius: 1, color: 'white', bgcolor: eventInfo.event.backgroundColor }}> {title} </Box>;
                }}
                height="auto"
                headerToolbar={{ left: 'prev,next today', center: 'title', right: 'dayGridMonth' }}
                dateClick={(info) => { setCurrentDate(info.date); }}
            />
            {renderEvents()}
        </Box>
    );
};

export default TeamCalendar; 