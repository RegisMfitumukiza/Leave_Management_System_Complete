import React, { useEffect, useState } from 'react';
import {
    Box,
    Typography,
    Paper,
    Grid,
    Card,
    CardContent,
    CircularProgress,
    Button,
    Alert,
    Avatar,
    Tooltip,
    IconButton,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Chip,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogContentText,
    DialogActions,
    LinearProgress,
    Stack
} from '@mui/material';
import {
    EventAvailable as EventAvailableIcon,
    EventBusy as EventBusyIcon,
    Warning as WarningIcon,
    TrendingUp as TrendingUpIcon,
    Add as AddIcon,
    History as HistoryIcon,
    Description as DescriptionIcon,
    CheckCircle as CheckCircleIcon,
    Cancel as CancelIcon,
    Pending as PendingIcon,
    Person as PersonIcon,
    Logout as LogoutIcon,
    Event as EventIcon,
    Info as InfoIcon
} from '@mui/icons-material';
import { leaveApi } from '@/lib/api';
import { useAuth } from '../../contexts/AuthContext';
import dayjs from 'dayjs';
import { useNavigate } from 'react-router-dom';
import NotificationBell from './NotificationBell';
import Calendar from 'react-calendar';
import 'react-calendar/dist/Calendar.css';

const StaffDashboard = () => {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [holidays, setHolidays] = useState([]);
    const [selectedDate, setSelectedDate] = useState(new Date());
    const [stats, setStats] = useState({
        leaveBalances: [],
        upcomingLeaves: [],
        recentApplications: [],
        teamOnLeave: [],
        upcomingHolidays: [],
        unreadNotifications: 0
    });
    const [selectedHoliday, setSelectedHoliday] = useState(null);
    const [holidayDialogOpen, setHolidayDialogOpen] = useState(false);
    const [selectedApp, setSelectedApp] = useState(null);
    const [modalOpen, setModalOpen] = useState(false);

    // Helper: get all holiday dates as Date objects
    const holidayDates = stats.upcomingHolidays.map(h => new Date(h.date));
    // Helper: get holiday by date
    const getHolidayByDate = (date) => {
        const d = dayjs(date).format('YYYY-MM-DD');
        return stats.upcomingHolidays.find(h => dayjs(h.date).format('YYYY-MM-DD') === d);
    };

    const fetchBalances = async () => {
        try {
            const response = await leaveApi.get('/leave-balances/me');
            setStats(prev => ({
                ...prev,
                leaveBalances: response.data
            }));
        } catch (error) {
            console.error('Error fetching leave balances:', error);
        }
    };

    const fetchHolidays = async () => {
        try {
            const startDate = dayjs().startOf('month').format('YYYY-MM-DD');
            const endDate = dayjs().add(3, 'month').endOf('month').format('YYYY-MM-DD');
            const response = await leaveApi.get('/holidays/range', {
                params: { start: startDate, end: endDate }
            });
            setHolidays(response.data);
        } catch (error) {
            console.error('Error fetching holidays:', error);
        }
    };

    const fetchRecentApplications = async () => {
        try {
            const response = await leaveApi.get(`/leaves?userId=${user.id}&limit=5`);
            setStats(prev => ({
                ...prev,
                recentApplications: response.data
            }));
        } catch (error) {
            console.error('Error fetching recent applications:', error);
        }
    };

    const fetchTeamCalendar = async () => {
        try {
            const response = await leaveApi.get('/leaves/team-calendar/staff');
            setStats(prev => ({
                ...prev,
                teamOnLeave: response.data
            }));
        } catch (error) {
            console.error('Error fetching team calendar:', error);
        }
    };

    useEffect(() => {
        const fetchData = async () => {
            try {
                setLoading(true);
                await Promise.all([
                    fetchBalances(),
                    fetchRecentApplications(),
                    fetchTeamCalendar(),
                    fetchHolidays()
                ]);
            } catch (error) {
                setError(error.message);
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, []);

    const getStatusColor = (status) => {
        switch (status) {
            case 'APPROVED':
                return 'success';
            case 'REJECTED':
                return 'error';
            case 'PENDING':
                return 'warning';
            default:
                return 'default';
        }
    };

    const getStatusIcon = (status) => {
        switch (status) {
            case 'APPROVED':
                return <CheckCircleIcon />;
            case 'REJECTED':
                return <CancelIcon />;
            case 'PENDING':
                return <PendingIcon />;
            default:
                return null;
        }
    };

    const handleLogout = () => {
        logout();
        navigate('/auth/login');
    };

    // Custom tile content for calendar
    const tileContent = ({ date }) => {
        const holiday = holidays.find(h => dayjs(h.date).isSame(date, 'day'));
        return holiday ? (
            <Tooltip title={holiday.name}>
                <EventIcon color="primary" fontSize="small" />
            </Tooltip>
        ) : null;
    };

    const LeaveBalanceCard = ({ balances }) => {
        const calculateMonthlyAccrual = (totalDays) => {
            return (totalDays / 12).toFixed(2);
        };

        const getProgressColor = (used, total) => {
            const percentage = (used / total) * 100;
            if (percentage >= 80) return 'error';
            if (percentage >= 60) return 'warning';
            return 'success';
        };

        return (
            <Paper sx={{ p: 3, mb: 3 }}>
                <Typography variant="h6" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                    Leave Balances
                    <Tooltip title="Leave accrues at 1.66 days per month">
                        <InfoIcon fontSize="small" color="action" />
                    </Tooltip>
                </Typography>
                <Grid container spacing={2}>
                    {balances.map((balance) => (
                        <Grid item xs={12} md={6} key={balance.leaveTypeId}>
                            <Card variant="outlined" sx={{ height: '100%' }}>
                                <CardContent>
                                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                                        <Typography variant="subtitle1" fontWeight="bold">
                                            {balance.leaveTypeName}
                                        </Typography>
                                        <Chip
                                            size="small"
                                            label={`${balance.remainingDays.toFixed(1)} days left`}
                                            color={getProgressColor(balance.usedDays, balance.totalDays)}
                                        />
                                    </Box>

                                    <Box sx={{ mb: 2 }}>
                                        <LinearProgress
                                            variant="determinate"
                                            value={(balance.usedDays / balance.totalDays) * 100}
                                            color={getProgressColor(balance.usedDays, balance.totalDays)}
                                            sx={{ height: 8, borderRadius: 4 }}
                                        />
                                    </Box>

                                    <Grid container spacing={1}>
                                        <Grid item xs={6}>
                                            <Typography variant="body2" color="text.secondary">
                                                Total Days
                                            </Typography>
                                            <Typography variant="body1">
                                                {balance.totalDays.toFixed(1)}
                                            </Typography>
                                        </Grid>
                                        <Grid item xs={6}>
                                            <Typography variant="body2" color="text.secondary">
                                                Used Days
                                            </Typography>
                                            <Typography variant="body1">
                                                {balance.usedDays.toFixed(1)}
                                            </Typography>
                                        </Grid>
                                        <Grid item xs={6}>
                                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                                                <TrendingUpIcon fontSize="small" color="action" />
                                                <Typography variant="body2" color="text.secondary">
                                                    Monthly Accrual
                                                </Typography>
                                            </Box>
                                            <Typography variant="body1">
                                                {calculateMonthlyAccrual(balance.totalDays)} days
                                            </Typography>
                                        </Grid>
                                        <Grid item xs={6}>
                                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                                                <HistoryIcon fontSize="small" color="action" />
                                                <Typography variant="body2" color="text.secondary">
                                                    Carried Over
                                                </Typography>
                                            </Box>
                                            <Typography variant="body1">
                                                {balance.carriedOverDays.toFixed(1)} days
                                            </Typography>
                                        </Grid>
                                    </Grid>
                                </CardContent>
                            </Card>
                        </Grid>
                    ))}
                </Grid>
            </Paper>
        );
    };

    if (loading) {
        return (
            <Box display="flex" justifyContent="center" alignItems="center" minHeight="80vh">
                <CircularProgress />
            </Box>
        );
    }

    if (error) {
        return (
            <Box p={3}>
                <Alert severity="error">{error}</Alert>
            </Box>
        );
    }

    return (
        <Box p={3}>
            {/* Topbar with NotificationBell */}
            <Box sx={{ mb: 2 }}>
                <Typography variant="h5">Welcome, {user?.firstName}!</Typography>
            </Box>

            {/* Quick Actions Bar */}
            <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
                <Button
                    variant="contained"
                    startIcon={<AddIcon />}
                    onClick={() => navigate('/staff/apply-leave')}
                >
                    Apply Leave
                </Button>
                <Button
                    variant="outlined"
                    startIcon={<HistoryIcon />}
                    onClick={() => navigate('/staff/leave-history')}
                >
                    View History
                </Button>
            </Box>

            {/* Leave Balances */}
            <LeaveBalanceCard balances={stats.leaveBalances} />

            {/* Recent Applications and Team Calendar */}
            <Grid container spacing={3}>
                <Grid item xs={12} md={6}>
                    <Paper sx={{ p: 3, height: '100%' }}>
                        <Typography variant="h6" sx={{ mb: 2 }}>Recent Applications</Typography>
                        {stats.recentApplications.length === 0 ? (
                            <Alert severity="info">No recent leave applications</Alert>
                        ) : (
                            <TableContainer>
                                <Table size="small">
                                    <TableHead>
                                        <TableRow>
                                            <TableCell>Type</TableCell>
                                            <TableCell>Dates</TableCell>
                                            <TableCell>Status</TableCell>
                                            <TableCell>Actions</TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {stats.recentApplications.map((app) => (
                                            <TableRow key={app.id}>
                                                <TableCell>{app.leaveTypeName}</TableCell>
                                                <TableCell>
                                                    {dayjs(app.startDate).format('MMM D')} - {dayjs(app.endDate).format('MMM D, YYYY')}
                                                </TableCell>
                                                <TableCell>
                                                    <Chip
                                                        icon={getStatusIcon(app.status)}
                                                        label={app.status}
                                                        color={getStatusColor(app.status)}
                                                        size="small"
                                                    />
                                                </TableCell>
                                                <TableCell>
                                                    <Tooltip title="View Details">
                                                        <IconButton
                                                            size="small"
                                                            onClick={() => {
                                                                setSelectedApp(app);
                                                                setModalOpen(true);
                                                            }}
                                                        >
                                                            <DescriptionIcon />
                                                        </IconButton>
                                                    </Tooltip>
                                                </TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                        )}
                    </Paper>
                </Grid>

                <Grid item xs={12} md={6}>
                    <Paper sx={{ p: 3, height: '100%' }}>
                        <Typography variant="h6" sx={{ mb: 2 }}>Team on Leave</Typography>
                        {stats.teamOnLeave.length === 0 ? (
                            <Alert severity="info">No team members are on leave today</Alert>
                        ) : (
                            <Grid container spacing={2}>
                                {stats.teamOnLeave.map((member) => (
                                    <Grid item xs={12} sm={6} key={member.id}>
                                        <Card>
                                            <CardContent>
                                                <Box display="flex" alignItems="center">
                                                    <Avatar
                                                        src={member.avatarUrl}
                                                        sx={{ width: 40, height: 40, mr: 2 }}
                                                    >
                                                        <PersonIcon />
                                                    </Avatar>
                                                    <Box>
                                                        <Typography variant="subtitle2">
                                                            {member.firstName} {member.lastName}
                                                        </Typography>
                                                        <Typography variant="body2" color="text.secondary">
                                                            {member.leaveTypeName}
                                                        </Typography>
                                                        <Typography variant="caption" color="text.secondary">
                                                            Until {dayjs(member.endDate).format('MMM D, YYYY')}
                                                        </Typography>
                                                    </Box>
                                                </Box>
                                            </CardContent>
                                        </Card>
                                    </Grid>
                                ))}
                            </Grid>
                        )}
                    </Paper>
                </Grid>
            </Grid>

            {/* Public Holidays Calendar */}
            <Paper sx={{ p: 3, mb: 3, mt: 3 }}>
                <Typography variant="h6" sx={{ mb: 2 }}>Public Holidays</Typography>
                <Grid container spacing={2}>
                    <Grid item xs={12} md={8}>
                        <Calendar
                            onChange={setSelectedDate}
                            value={selectedDate}
                            tileContent={({ date }) => {
                                const holiday = holidays.find(h => dayjs(h.date).isSame(date, 'day'));
                                return holiday ? (
                                    <Tooltip title={holiday.name}>
                                        <EventIcon color="primary" fontSize="small" />
                                    </Tooltip>
                                ) : null;
                            }}
                            minDate={new Date()}
                            maxDate={dayjs().add(3, 'month').toDate()}
                        />
                    </Grid>
                    <Grid item xs={12} md={4}>
                        <Typography variant="subtitle1" sx={{ mb: 1 }}>Upcoming Holidays</Typography>
                        {holidays
                            .filter(h => dayjs(h.date).isAfter(dayjs(), 'day'))
                            .sort((a, b) => dayjs(a.date).diff(dayjs(b.date)))
                            .slice(0, 5)
                            .map(holiday => (
                                <Card key={holiday.id} sx={{ mb: 1 }}>
                                    <CardContent>
                                        <Typography variant="subtitle2">
                                            {holiday.name}
                                        </Typography>
                                        <Typography variant="body2" color="text.secondary">
                                            {dayjs(holiday.date).format('MMMM D, YYYY')}
                                        </Typography>
                                        {holiday.description && (
                                            <Typography variant="caption" color="text.secondary">
                                                {holiday.description}
                                            </Typography>
                                        )}
                                    </CardContent>
                                </Card>
                            ))}
                    </Grid>
                </Grid>
            </Paper>

            {/* Upcoming Holidays */}
            {/* <Paper sx={{ p: 3, mt: 3 }}>
                <Typography variant="h6" sx={{ mb: 2 }}>Upcoming Holidays</Typography>
                {stats.upcomingHolidays.length === 0 ? (
                    <Alert severity="info">No upcoming holidays in the next 30 days</Alert>
                ) : (
                    <Box>
                        <Calendar
                            tileClassName={({ date, view }) => {
                                if (view === 'month' && holidayDates.some(d => dayjs(d).isSame(date, 'day'))) {
                                    return 'holiday-highlight';
                                }
                                return null;
                            }}
                            tileContent={({ date, view }) => {
                                if (view === 'month') {
                                    const holiday = getHolidayByDate(date);
                                    if (holiday) {
                                        return (
                                            <Tooltip title={holiday.name}>
                                                <span role="img" aria-label="holiday">🎉</span>
                                            </Tooltip>
                                        );
                                    }
                                }
                                return null;
                            }}
                            onClickDay={(date) => {
                                const holiday = getHolidayByDate(date);
                                if (holiday) {
                                    setSelectedHoliday(holiday);
                                    setHolidayDialogOpen(true);
                                }
                            }}
                        />
                        <style>{`
                            .holiday-highlight {
                                background: #ffe082 !important;
                                border-radius: 50%;
                            }
                        `}</style>
                        <Dialog open={holidayDialogOpen} onClose={() => setHolidayDialogOpen(false)}>
                            <DialogTitle>{selectedHoliday?.name}</DialogTitle>
                            <DialogContent>
                                <DialogContentText>
                                    {selectedHoliday?.description || 'No description.'}
                                </DialogContentText>
                                <DialogContentText sx={{ mt: 2 }}>
                                    Date: {selectedHoliday ? dayjs(selectedHoliday.date).format('MMMM D, YYYY') : ''}
                                </DialogContentText>
                            </DialogContent>
                            <DialogActions>
                                <Button onClick={() => setHolidayDialogOpen(false)}>Close</Button>
                            </DialogActions>
                        </Dialog>
                    </Box>
                )}
            </Paper> */}

            <Dialog open={modalOpen} onClose={() => setModalOpen(false)} maxWidth="sm" fullWidth>
                <DialogTitle>Leave Application Details</DialogTitle>
                <DialogContent dividers>
                    {selectedApp && (
                        <Box>
                            <Typography variant="subtitle1"><b>Type:</b> {selectedApp.leaveTypeName}</Typography>
                            <Typography variant="subtitle1"><b>Status:</b> <Chip label={selectedApp.status} color={getStatusColor(selectedApp.status)} size="small" /></Typography>
                            <Typography variant="subtitle1"><b>Dates:</b> {dayjs(selectedApp.startDate).format('MMM D, YYYY')} - {dayjs(selectedApp.endDate).format('MMM D, YYYY')}</Typography>
                            <Typography variant="subtitle1"><b>Reason:</b> {selectedApp.reason}</Typography>
                            <Typography variant="subtitle1"><b>Comments:</b> {selectedApp.comments || '—'}</Typography>
                            <Typography variant="subtitle1"><b>Documents:</b></Typography>
                            {selectedApp && selectedApp.documents && selectedApp.documents.length > 0 ? (
                                <Stack direction="row" spacing={1}>
                                    {selectedApp.documents.map(doc => (
                                        <Button key={doc.id} size="small" href={doc.url.startsWith('http') ? doc.url : `${import.meta.env.VITE_API_URL}${doc.url}`} target="_blank" startIcon={<DescriptionIcon />}>{doc.fileName}</Button>
                                    ))}
                                </Stack>
                            ) : '—'}
                        </Box>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setModalOpen(false)}>Close</Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default StaffDashboard; 