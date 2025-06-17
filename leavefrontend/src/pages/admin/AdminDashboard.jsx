import React, { useEffect, useState } from 'react';
import Sidebar from '../../components/Sidebar';
import Topbar from '../../components/Topbar';
import { Box, Typography, Paper, Grid, Card, CardContent, CircularProgress, Alert, Button, Dialog, DialogTitle, DialogContent, DialogActions, MenuItem, FormControl, InputLabel, Select, TextField, Snackbar, FormControlLabel, Switch, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Chip } from '@mui/material';
import { authApi, leaveApi } from '@/lib/api';
import dayjs from 'dayjs';
import IndividualLeaveCalendar from './IndividualLeaveCalendar';
import { Calendar, dateFnsLocalizer } from 'react-big-calendar';
import format from 'date-fns/format';
import parse from 'date-fns/parse';
import startOfWeek from 'date-fns/startOfWeek';
import getDay from 'date-fns/getDay';
import enUS from 'date-fns/locale/en-US';
import 'react-big-calendar/lib/css/react-big-calendar.css';
import NotificationsIcon from '@mui/icons-material/Notifications';
import DashboardIcon from '@mui/icons-material/Dashboard';
import PeopleIcon from '@mui/icons-material/People';
import BusinessIcon from '@mui/icons-material/Business';
import EventIcon from '@mui/icons-material/Event';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import AssessmentIcon from '@mui/icons-material/Assessment';
import SettingsIcon from '@mui/icons-material/Settings';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import { useAuth } from '../../contexts/AuthContext';

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

const adminNavItems = [
    { label: 'Dashboard', icon: <DashboardIcon />, path: '/admin/dashboard' },
    { label: 'User Management', icon: <PeopleIcon />, path: '/admin/users' },
    { label: 'Department Management', icon: <BusinessIcon />, path: '/admin/departments' },
    { label: 'Leave Types', icon: <EventIcon />, path: '/admin/leave-types' },
    { label: 'Holidays', icon: <CalendarMonthIcon />, path: '/admin/holidays' },
    { label: 'Reports', icon: <AssessmentIcon />, path: '/admin/reports' },
    { label: 'Team Calendar', icon: <EventIcon />, path: '/admin/team-calendar' },
    { label: 'Leave Analytics', icon: <AssessmentIcon />, path: '/admin/leave-analytics' },
    { label: 'Settings', icon: <SettingsIcon />, path: '/admin/settings' },
    { label: 'Profile', icon: <AccountCircleIcon />, path: '/admin/profile' },
];

const AdminDashboard = () => {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [openDialog, setOpenDialog] = useState(''); // '', 'user', 'department', 'leaveType', 'holiday'
    const [departments, setDepartments] = useState([]);
    const [userForm, setUserForm] = useState({
        firstName: '',
        lastName: '',
        email: '',
        password: '',
        role: 'STAFF',
        departmentId: ''
    });
    const [userLoading, setUserLoading] = useState(false);
    const [userError, setUserError] = useState('');
    const [userSuccess, setUserSuccess] = useState(false);
    const [managers, setManagers] = useState([]);
    const [deptForm, setDeptForm] = useState({
        name: '',
        description: '',
        managerId: ''
    });
    const [deptLoading, setDeptLoading] = useState(false);
    const [deptError, setDeptError] = useState('');
    const [deptSuccess, setDeptSuccess] = useState(false);
    const [leaveTypeForm, setLeaveTypeForm] = useState({
        name: '',
        description: '',
        defaultDays: '',
        isActive: true,
        requiresApproval: false,
        requiresDocumentation: false,
        isPaid: true,
        canCarryOver: false,
        maxCarryOverDays: ''
    });
    const [leaveTypeLoading, setLeaveTypeLoading] = useState(false);
    const [leaveTypeError, setLeaveTypeError] = useState('');
    const [leaveTypeSuccess, setLeaveTypeSuccess] = useState(false);
    const [holidayForm, setHolidayForm] = useState({
        name: '',
        date: '',
        description: '',
        isPublic: true
    });
    const [holidayLoading, setHolidayLoading] = useState(false);
    const [holidayError, setHolidayError] = useState('');
    const [holidaySuccess, setHolidaySuccess] = useState(false);
    const [pendingApprovals, setPendingApprovals] = useState([]);
    const [recentApplications, setRecentApplications] = useState([]);
    const [loadingApprovals, setLoadingApprovals] = useState(true);
    const [loadingApplications, setLoadingApplications] = useState(true);
    const [approvalError, setApprovalError] = useState('');
    const [applicationError, setApplicationError] = useState('');
    const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
    const [upcomingHolidays, setUpcomingHolidays] = useState([]);
    const [allHolidays, setAllHolidays] = useState([]);
    const [loadingHolidays, setLoadingHolidays] = useState(true);
    const [holidaysError, setHolidaysError] = useState('');
    const [deptOverview, setDeptOverview] = useState([]);
    const [loadingDeptOverview, setLoadingDeptOverview] = useState(true);
    const [deptOverviewError, setDeptOverviewError] = useState('');
    const [holidayDialogOpen, setHolidayDialogOpen] = useState(false);
    const [selectedHoliday, setSelectedHoliday] = useState(null);
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [notifAnchorEl, setNotifAnchorEl] = useState(null);
    const [notifLoading, setNotifLoading] = useState(false);
    const { user } = useAuth();

    const handleOpenDialog = (type) => setOpenDialog(type);
    const handleCloseDialog = () => setOpenDialog('');

    useEffect(() => {
        const fetchStats = async () => {
            try {
                const res = await leaveApi.get('/dashboard-stats');
                setStats(res.data);
            } catch (err) {
                setError('Failed to load dashboard stats');
            } finally {
                setLoading(false);
            }
        };
        fetchStats();
        // Fetch departments for user form
        authApi.get('/departments')
            .then(res => setDepartments(res.data))
            .catch(() => setDepartments([]));
        // Fetch managers for department form
        authApi.get('/users')
            .then(res => setManagers(res.data.filter(u => u.role === 'MANAGER')))
            .catch(() => setManagers([]));
        fetchPendingApprovals();
        fetchRecentApplications();
        fetchUpcomingHolidays();
        fetchAllHolidays();
        fetchDeptOverview();
        fetchNotifications();
    }, []);

    const fetchPendingApprovals = async () => {
        setLoadingApprovals(true);
        try {
            const res = await leaveApi.get('/leaves/pending');
            setPendingApprovals(res.data);
        } catch (err) {
            setApprovalError('Failed to load pending approvals');
        } finally {
            setLoadingApprovals(false);
        }
    };

    const fetchRecentApplications = async () => {
        setLoadingApplications(true);
        try {
            const res = await leaveApi.get('/leaves/recent');
            setRecentApplications(res.data);
        } catch (err) {
            setApplicationError('Failed to load recent applications');
        } finally {
            setLoadingApplications(false);
        }
    };

    const fetchUpcomingHolidays = async () => {
        setLoadingHolidays(true);
        setHolidaysError('');
        try {
            const today = dayjs().format('YYYY-MM-DD');
            const end = dayjs().add(60, 'day').format('YYYY-MM-DD');
            const res = await leaveApi.get('/holidays/range', { params: { start: today, end } });
            const sorted = res.data.sort((a, b) => new Date(a.date) - new Date(b.date));
            setUpcomingHolidays(sorted.slice(0, 5));
        } catch (err) {
            setHolidaysError('Failed to load upcoming holidays');
        } finally {
            setLoadingHolidays(false);
        }
    };

    const fetchAllHolidays = async () => {
        try {
            const res = await leaveApi.get('/holidays');
            setAllHolidays(res.data);
        } catch (err) {
            // Optionally handle error
        }
    };

    const fetchDeptOverview = async () => {
        setLoadingDeptOverview(true);
        setDeptOverviewError('');
        try {
            const res = await leaveApi.get('/leave-analytics/department-distribution');
            const data = Object.entries(res.data).map(([dept, stats]) => ({
                department: dept,
                ...stats
            }));
            setDeptOverview(data);
        } catch (err) {
            setDeptOverviewError('Failed to load department-wise leave overview');
        } finally {
            setLoadingDeptOverview(false);
        }
    };

    const handleUserFormChange = (e) => {
        const { name, value } = e.target;
        setUserForm((prev) => ({ ...prev, [name]: value }));
    };

    const handleAddUser = async (e) => {
        e.preventDefault();
        setUserLoading(true);
        setUserError('');
        try {
            const payload = {
                firstName: userForm.firstName,
                lastName: userForm.lastName,
                email: userForm.email,
                password: userForm.password,
                role: userForm.role,
                department: userForm.departmentId ? { id: userForm.departmentId } : undefined
            };
            await authApi.post('/register', payload);
            setUserSuccess(true);
            setOpenDialog('');
            setUserForm({ firstName: '', lastName: '', email: '', password: '', role: 'STAFF', departmentId: '' });
        } catch (err) {
            setUserError(err.response?.data || 'Failed to add user');
        } finally {
            setUserLoading(false);
        }
    };

    const handleDeptFormChange = (e) => {
        const { name, value } = e.target;
        setDeptForm((prev) => ({ ...prev, [name]: value }));
    };

    const handleAddDepartment = async (e) => {
        e.preventDefault();
        setDeptLoading(true);
        setDeptError('');
        try {
            const payload = {
                name: deptForm.name,
                description: deptForm.description,
                manager: deptForm.managerId ? { id: deptForm.managerId } : undefined
            };
            await authApi.post('/departments', payload);
            setDeptSuccess(true);
            setOpenDialog('');
            setDeptForm({ name: '', description: '', managerId: '' });
        } catch (err) {
            setDeptError(err.response?.data || 'Failed to add department');
        } finally {
            setDeptLoading(false);
        }
    };

    const handleLeaveTypeFormChange = (e) => {
        const { name, value, type, checked } = e.target;
        setLeaveTypeForm((prev) => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    const handleAddLeaveType = async (e) => {
        e.preventDefault();
        setLeaveTypeLoading(true);
        setLeaveTypeError('');
        try {
            const payload = {
                name: leaveTypeForm.name,
                description: leaveTypeForm.description,
                defaultDays: Number(leaveTypeForm.defaultDays),
                isActive: leaveTypeForm.isActive,
                requiresApproval: leaveTypeForm.requiresApproval,
                requiresDocumentation: leaveTypeForm.requiresDocumentation,
                isPaid: leaveTypeForm.isPaid,
                canCarryOver: leaveTypeForm.canCarryOver,
                maxCarryOverDays: leaveTypeForm.canCarryOver ? Number(leaveTypeForm.maxCarryOverDays) : 0
            };
            await leaveApi.post('/leave-types', payload);
            setLeaveTypeSuccess(true);
            setOpenDialog('');
            setLeaveTypeForm({
                name: '',
                description: '',
                defaultDays: '',
                isActive: true,
                requiresApproval: false,
                requiresDocumentation: false,
                isPaid: true,
                canCarryOver: false,
                maxCarryOverDays: ''
            });
        } catch (err) {
            setLeaveTypeError(err.response?.data || 'Failed to add leave type');
        } finally {
            setLeaveTypeLoading(false);
        }
    };

    const handleHolidayFormChange = (e) => {
        const { name, value, checked } = e.target;
        setHolidayForm(prev => ({
            ...prev,
            [name]: name === 'isPublic' ? checked : value
        }));
    };

    const handleAddHoliday = async (e) => {
        e.preventDefault();
        setHolidayLoading(true);
        setHolidayError('');

        try {
            await leaveApi.post('/holidays', holidayForm);
            setHolidaySuccess(true);
            handleCloseDialog();
            setHolidayForm({
                name: '',
                date: '',
                description: '',
                isPublic: true
            });
            fetchStats();
        } catch (err) {
            setHolidayError(err.response?.data?.message || 'Failed to add holiday');
        } finally {
            setHolidayLoading(false);
        }
    };

    const handleApproveLeave = async (applicationId, comments) => {
        try {
            await leaveApi.post(`/leaves/${applicationId}/approve`, { comments });
            fetchPendingApprovals();
            fetchRecentApplications();
            setSnackbar({ open: true, message: 'Leave application approved', severity: 'success' });
        } catch (err) {
            setSnackbar({ open: true, message: 'Failed to approve leave application', severity: 'error' });
        }
    };

    const handleRejectLeave = async (applicationId, comments) => {
        try {
            await leaveApi.post(`/leaves/${applicationId}/reject`, { comments });
            fetchPendingApprovals();
            fetchRecentApplications();
            setSnackbar({ open: true, message: 'Leave application rejected', severity: 'success' });
        } catch (err) {
            setSnackbar({ open: true, message: 'Failed to reject leave application', severity: 'error' });
        }
    };

    const fetchNotifications = async () => {
        setNotifLoading(true);
        try {
            // Replace with your actual API endpoint for admin notifications
            const res = await leaveApi.get('/notifications');
            setNotifications(res.data);
            setUnreadCount(res.data.filter(n => !n.read).length);
        } catch {
            setNotifications([]);
            setUnreadCount(0);
        } finally {
            setNotifLoading(false);
        }
    };

    const handleNotificationsClick = (e) => {
        setNotifAnchorEl(e.currentTarget);
    };
    const handleCloseNotifications = () => {
        setNotifAnchorEl(null);
    };
    const handleMarkAllRead = async () => {
        // Replace with your actual API endpoint for marking all as read
        await leaveApi.post('/notifications/mark-all-read');
        fetchNotifications();
    };
    const handleMarkRead = async (id) => {
        // Replace with your actual API endpoint for marking one as read
        await leaveApi.post(`/notifications/${id}/mark-read`);
        fetchNotifications();
    };

    const handleFixDepartments = async () => {
        try {
            setSnackbar({ open: true, message: 'Fixing department assignments...', severity: 'info' });
            const response = await authApi.post('/users/fix-departments');
            setSnackbar({
                open: true,
                message: `Successfully fixed ${response.data.fixedCount} users without department assignment`,
                severity: 'success'
            });
        } catch (error) {
            setSnackbar({
                open: true,
                message: error.response?.data?.message || 'Failed to fix department assignments',
                severity: 'error'
            });
        }
    };

    const handleSyncProfilePictures = async () => {
        try {
            setSnackbar({ open: true, message: 'Syncing profile pictures...', severity: 'info' });
            const response = await authApi.post('/users/sync-profile-pictures');
            setSnackbar({
                open: true,
                message: `Found ${response.data.syncedCount} users with Google IDs but no avatars`,
                severity: 'success'
            });
        } catch (error) {
            setSnackbar({
                open: true,
                message: error.response?.data?.message || 'Failed to sync profile pictures',
                severity: 'error'
            });
        }
    };

    return (
        <Box sx={{ display: 'flex', minHeight: '100vh', background: '#f7f7f7' }}>
            <Sidebar items={adminNavItems} />
            <Box sx={{ flexGrow: 1 }}>
                <Topbar
                    title="Admin Dashboard"
                    avatarLetter={user?.firstName?.[0] || 'A'}
                    avatarUrl={user?.avatarUrl || ''}
                    showNotifications={true}
                    notifications={notifications}
                    unreadCount={unreadCount}
                    onNotificationsClick={handleNotificationsClick}
                    onMarkAllRead={handleMarkAllRead}
                    onMarkRead={handleMarkRead}
                    loading={notifLoading}
                    anchorEl={notifAnchorEl}
                    onCloseNotifications={handleCloseNotifications}
                />
                <Box sx={{ p: 4 }}>
                    <Typography variant="h4" sx={{ mb: 3 }}>
                        Welcome, Admin
                    </Typography>
                    <Paper sx={{ p: 3, mb: 2 }}>
                        {loading ? (
                            <Box display="flex" justifyContent="center" alignItems="center" minHeight="100px">
                                <CircularProgress />
                            </Box>
                        ) : error ? (
                            <Alert severity="error">{error}</Alert>
                        ) : (
                            <Grid container spacing={3}>
                                <Grid item xs={12} sm={6} md={3}>
                                    <Card>
                                        <CardContent>
                                            <Typography color="text.secondary" gutterBottom>
                                                Total Staff
                                            </Typography>
                                            <Typography variant="h4">
                                                {stats?.totalUsers ?? 0}
                                            </Typography>
                                        </CardContent>
                                    </Card>
                                </Grid>
                                <Grid item xs={12} sm={6} md={3}>
                                    <Card>
                                        <CardContent>
                                            <Typography color="text.secondary" gutterBottom>
                                                Active Leave Requests
                                            </Typography>
                                            <Typography variant="h4">
                                                {stats?.activeLeaveRequests ?? 0}
                                            </Typography>
                                        </CardContent>
                                    </Card>
                                </Grid>
                                <Grid item xs={12} sm={6} md={3}>
                                    <Card>
                                        <CardContent>
                                            <Typography color="text.secondary" gutterBottom>
                                                Departments
                                            </Typography>
                                            <Typography variant="h4">
                                                {stats?.departmentCount ?? 0}
                                            </Typography>
                                        </CardContent>
                                    </Card>
                                </Grid>
                                <Grid item xs={12} sm={6} md={3}>
                                    <Card>
                                        <CardContent>
                                            <Typography color="text.secondary" gutterBottom>
                                                Leave Types
                                            </Typography>
                                            <Typography variant="h4">
                                                {stats?.leaveTypeCount ?? 0}
                                            </Typography>
                                        </CardContent>
                                    </Card>
                                </Grid>
                            </Grid>
                        )}
                    </Paper>
                    {/* Quick Actions */}
                    <Box sx={{ display: 'flex', gap: 2, mb: 4, flexWrap: 'wrap' }}>
                        <Button variant="contained" onClick={() => handleOpenDialog('user')}>Add User</Button>
                        <Button variant="contained" onClick={() => handleOpenDialog('department')}>Add Department</Button>
                        <Button variant="contained" onClick={() => handleOpenDialog('leaveType')}>Add Leave Type</Button>
                        <Button variant="contained" onClick={() => handleOpenDialog('holiday')}>Add Holiday</Button>
                        <Button variant="outlined" color="warning" onClick={handleFixDepartments}>Fix Department Assignments</Button>
                        <Button variant="outlined" color="info" onClick={handleSyncProfilePictures}>Sync Profile Pictures</Button>
                    </Box>
                    {/* Recent Leave Applications */}
                    <Paper sx={{ p: 3, mb: 4 }}>
                        <Typography variant="h6" sx={{ mb: 2 }}>Recent Leave Applications</Typography>
                        {loadingApplications ? (
                            <Box display="flex" justifyContent="center" p={3}>
                                <CircularProgress />
                            </Box>
                        ) : applicationError ? (
                            <Alert severity="error">{applicationError}</Alert>
                        ) : recentApplications.length === 0 ? (
                            <Alert severity="info">No recent applications</Alert>
                        ) : (
                            <TableContainer>
                                <Table size="small">
                                    <TableHead>
                                        <TableRow>
                                            <TableCell>Employee</TableCell>
                                            <TableCell>Leave Type</TableCell>
                                            <TableCell>Start Date</TableCell>
                                            <TableCell>End Date</TableCell>
                                            <TableCell>Status</TableCell>
                                            <TableCell>Applied On</TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {recentApplications.map((app) => (
                                            <TableRow key={app.id}>
                                                <TableCell>{app.employeeName || app.userName || 'User'}</TableCell>
                                                <TableCell>{app.leaveType || app.leaveTypeName || ''}</TableCell>
                                                <TableCell>{dayjs(app.startDate).format('MMM D, YYYY')}</TableCell>
                                                <TableCell>{dayjs(app.endDate).format('MMM D, YYYY')}</TableCell>
                                                <TableCell>{app.status}</TableCell>
                                                <TableCell>{dayjs(app.createdAt).format('MMM D, YYYY')}</TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                        )}
                    </Paper>
                    {/* Individual Leave Calendar */}
                    <Paper sx={{ p: 3, mb: 4 }}>
                        <IndividualLeaveCalendar />
                    </Paper>
                    {/* Upcoming Holidays Widget */}
                    <Paper sx={{ p: 3, mb: 4 }}>
                        <Typography variant="h6" sx={{ mb: 2 }}>Upcoming Holidays</Typography>
                        {loadingHolidays ? (
                            <Box display="flex" justifyContent="center" p={3}>
                                <CircularProgress />
                            </Box>
                        ) : holidaysError ? (
                            <Alert severity="error">{holidaysError}</Alert>
                        ) : upcomingHolidays.length === 0 ? (
                            <Alert severity="info">No upcoming holidays</Alert>
                        ) : (
                            <>
                                <Box>
                                    {upcomingHolidays.slice(0, 5).map((holiday) => (
                                        <Box key={holiday.id} sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 2 }}>
                                            <Typography variant="subtitle1" sx={{ minWidth: 120 }}>{dayjs(holiday.date).format('MMM D, YYYY')}</Typography>
                                            <Typography variant="body1" sx={{ fontWeight: 500 }}>{holiday.name}</Typography>
                                            <Typography variant="caption" color="text.secondary">{holiday.isPublic ? 'Public' : 'Optional'}</Typography>
                                        </Box>
                                    ))}
                                </Box>
                                {/* Holiday Calendar View */}
                                <Box sx={{ mt: 4 }}>
                                    <Typography variant="subtitle1" sx={{ mb: 2 }}>Holiday Calendar</Typography>
                                    <Calendar
                                        localizer={localizer}
                                        events={allHolidays.map(h => ({
                                            id: h.id,
                                            title: h.name,
                                            start: new Date(h.date),
                                            end: new Date(h.date),
                                            allDay: true,
                                            isPublic: h.public === undefined ? h.isPublic : h.public,
                                            description: h.description
                                        }))}
                                        startAccessor="start"
                                        endAccessor="end"
                                        style={{ height: 400 }}
                                        eventPropGetter={(event) => {
                                            let backgroundColor = event.isPublic ? '#1976d2' : '#ff9800';
                                            return {
                                                style: {
                                                    backgroundColor,
                                                    borderRadius: '3px',
                                                    opacity: 0.9,
                                                    color: 'white',
                                                    border: '0px',
                                                    display: 'block',
                                                }
                                            };
                                        }}
                                        views={['month']}
                                        defaultView="month"
                                        tooltipAccessor={(event) => `${event.title} (${event.isPublic ? 'Public' : 'Optional'})`}
                                        onSelectEvent={(event) => {
                                            setSelectedHoliday(event);
                                            setHolidayDialogOpen(true);
                                        }}
                                    />
                                    {/* Legend */}
                                    <Box sx={{ mt: 2, display: 'flex', gap: 3, alignItems: 'center' }}>
                                        <Typography variant="subtitle2">Legend:</Typography>
                                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                            <Box sx={{ width: 20, height: 20, bgcolor: '#1976d2', borderRadius: 1 }} />
                                            <Typography variant="body2">Public Holiday</Typography>
                                        </Box>
                                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                            <Box sx={{ width: 20, height: 20, bgcolor: '#ff9800', borderRadius: 1 }} />
                                            <Typography variant="body2">Optional Holiday</Typography>
                                        </Box>
                                    </Box>
                                </Box>
                            </>
                        )}
                    </Paper>
                    {/* Department-wise Leave Overview */}
                    <Paper sx={{ p: 3, mb: 4 }}>
                        <Typography variant="h6" sx={{ mb: 2 }}>Department-wise Leave Overview</Typography>
                        {loadingDeptOverview ? (
                            <Box display="flex" justifyContent="center" p={3}>
                                <CircularProgress />
                            </Box>
                        ) : deptOverviewError ? (
                            <Alert severity="error">{deptOverviewError}</Alert>
                        ) : deptOverview.length === 0 ? (
                            <Alert severity="info">No department data available</Alert>
                        ) : (
                            <TableContainer>
                                <Table size="small">
                                    <TableHead>
                                        <TableRow>
                                            <TableCell>Department</TableCell>
                                            <TableCell align="right">Total Users</TableCell>
                                            <TableCell align="right">Total Leave Days</TableCell>
                                            <TableCell align="right">Used Leave Days</TableCell>
                                            <TableCell align="right">Remaining Leave Days</TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {deptOverview.map((row) => (
                                            <TableRow key={row.department}>
                                                <TableCell>{row.department}</TableCell>
                                                <TableCell align="right">{row.totalUsers}</TableCell>
                                                <TableCell align="right">{row.totalDays}</TableCell>
                                                <TableCell align="right">{row.usedDays}</TableCell>
                                                <TableCell align="right">{row.remainingDays}</TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                        )}
                    </Paper>
                    {/* Dialogs */}
                    <Dialog open={openDialog === 'user'} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
                        <DialogTitle>Add User</DialogTitle>
                        <DialogContent>
                            <form onSubmit={handleAddUser} id="add-user-form">
                                <TextField
                                    margin="normal"
                                    fullWidth
                                    label="First Name"
                                    name="firstName"
                                    value={userForm.firstName}
                                    onChange={handleUserFormChange}
                                    required
                                />
                                <TextField
                                    margin="normal"
                                    fullWidth
                                    label="Last Name"
                                    name="lastName"
                                    value={userForm.lastName}
                                    onChange={handleUserFormChange}
                                    required
                                />
                                <TextField
                                    margin="normal"
                                    fullWidth
                                    label="Email"
                                    name="email"
                                    type="email"
                                    value={userForm.email}
                                    onChange={handleUserFormChange}
                                    required
                                />
                                <TextField
                                    margin="normal"
                                    fullWidth
                                    label="Password"
                                    name="password"
                                    type="password"
                                    value={userForm.password}
                                    onChange={handleUserFormChange}
                                    required
                                />
                                <FormControl fullWidth margin="normal">
                                    <InputLabel>Role</InputLabel>
                                    <Select
                                        name="role"
                                        value={userForm.role}
                                        label="Role"
                                        onChange={handleUserFormChange}
                                    >
                                        <MenuItem value="STAFF">Staff</MenuItem>
                                        <MenuItem value="MANAGER">Manager</MenuItem>
                                        <MenuItem value="ADMIN">Admin</MenuItem>
                                    </Select>
                                </FormControl>
                                <FormControl fullWidth margin="normal">
                                    <InputLabel>Department</InputLabel>
                                    <Select
                                        name="departmentId"
                                        value={userForm.departmentId}
                                        label="Department"
                                        onChange={handleUserFormChange}
                                    >
                                        <MenuItem value="">None</MenuItem>
                                        {departments.map((dept) => (
                                            <MenuItem key={dept.id} value={dept.id}>{dept.name}</MenuItem>
                                        ))}
                                    </Select>
                                </FormControl>
                                {userError && <Alert severity="error" sx={{ mt: 2 }}>{userError}</Alert>}
                            </form>
                        </DialogContent>
                        <DialogActions>
                            <Button onClick={handleCloseDialog} disabled={userLoading}>Cancel</Button>
                            <Button
                                variant="contained"
                                type="submit"
                                form="add-user-form"
                                disabled={userLoading}
                            >
                                {userLoading ? <CircularProgress size={20} /> : 'Add'}
                            </Button>
                        </DialogActions>
                    </Dialog>
                    <Dialog open={openDialog === 'department'} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
                        <DialogTitle>Add Department</DialogTitle>
                        <DialogContent>
                            <form onSubmit={handleAddDepartment} id="add-dept-form">
                                <TextField
                                    margin="normal"
                                    fullWidth
                                    label="Department Name"
                                    name="name"
                                    value={deptForm.name}
                                    onChange={handleDeptFormChange}
                                    required
                                />
                                <TextField
                                    margin="normal"
                                    fullWidth
                                    label="Description"
                                    name="description"
                                    value={deptForm.description}
                                    onChange={handleDeptFormChange}
                                />
                                <FormControl fullWidth margin="normal">
                                    <InputLabel>Manager</InputLabel>
                                    <Select
                                        name="managerId"
                                        value={deptForm.managerId}
                                        label="Manager"
                                        onChange={handleDeptFormChange}
                                    >
                                        <MenuItem value="">None</MenuItem>
                                        {managers.map((mgr) => (
                                            <MenuItem key={mgr.id} value={mgr.id}>{mgr.name || mgr.email}</MenuItem>
                                        ))}
                                    </Select>
                                </FormControl>
                                {deptError && <Alert severity="error" sx={{ mt: 2 }}>{deptError}</Alert>}
                            </form>
                        </DialogContent>
                        <DialogActions>
                            <Button onClick={handleCloseDialog} disabled={deptLoading}>Cancel</Button>
                            <Button
                                variant="contained"
                                type="submit"
                                form="add-dept-form"
                                disabled={deptLoading}
                            >
                                {deptLoading ? <CircularProgress size={20} /> : 'Add'}
                            </Button>
                        </DialogActions>
                    </Dialog>
                    <Dialog open={openDialog === 'leaveType'} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
                        <DialogTitle>Add Leave Type</DialogTitle>
                        <DialogContent>
                            <form onSubmit={handleAddLeaveType} id="add-leave-type-form">
                                <TextField
                                    margin="normal"
                                    fullWidth
                                    label="Leave Type Name"
                                    name="name"
                                    value={leaveTypeForm.name}
                                    onChange={handleLeaveTypeFormChange}
                                    required
                                />
                                <TextField
                                    margin="normal"
                                    fullWidth
                                    label="Description"
                                    name="description"
                                    value={leaveTypeForm.description}
                                    onChange={handleLeaveTypeFormChange}
                                />
                                <TextField
                                    margin="normal"
                                    fullWidth
                                    label="Default Days"
                                    name="defaultDays"
                                    type="number"
                                    value={leaveTypeForm.defaultDays}
                                    onChange={handleLeaveTypeFormChange}
                                    required
                                />
                                <FormControl fullWidth margin="normal">
                                    <InputLabel shrink>Is Active</InputLabel>
                                    <Select
                                        name="isActive"
                                        value={leaveTypeForm.isActive}
                                        label="Is Active"
                                        onChange={handleLeaveTypeFormChange}
                                    >
                                        <MenuItem value={true}>Yes</MenuItem>
                                        <MenuItem value={false}>No</MenuItem>
                                    </Select>
                                </FormControl>
                                <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mt: 2 }}>
                                    <FormControl>
                                        <label>
                                            <input
                                                type="checkbox"
                                                name="requiresApproval"
                                                checked={leaveTypeForm.requiresApproval}
                                                onChange={handleLeaveTypeFormChange}
                                            /> Requires Approval
                                        </label>
                                    </FormControl>
                                    <FormControl>
                                        <label>
                                            <input
                                                type="checkbox"
                                                name="requiresDocumentation"
                                                checked={leaveTypeForm.requiresDocumentation}
                                                onChange={handleLeaveTypeFormChange}
                                            /> Requires Documentation
                                        </label>
                                    </FormControl>
                                    <FormControl>
                                        <label>
                                            <input
                                                type="checkbox"
                                                name="isPaid"
                                                checked={leaveTypeForm.isPaid}
                                                onChange={handleLeaveTypeFormChange}
                                            /> Is Paid
                                        </label>
                                    </FormControl>
                                    <FormControl>
                                        <label>
                                            <input
                                                type="checkbox"
                                                name="canCarryOver"
                                                checked={leaveTypeForm.canCarryOver}
                                                onChange={handleLeaveTypeFormChange}
                                            /> Can Carry Over
                                        </label>
                                    </FormControl>
                                </Box>
                                {leaveTypeForm.canCarryOver && (
                                    <TextField
                                        margin="normal"
                                        fullWidth
                                        label="Max Carry Over Days"
                                        name="maxCarryOverDays"
                                        type="number"
                                        value={leaveTypeForm.maxCarryOverDays}
                                        onChange={handleLeaveTypeFormChange}
                                        required
                                    />
                                )}
                                {leaveTypeError && <Alert severity="error" sx={{ mt: 2 }}>{leaveTypeError}</Alert>}
                            </form>
                        </DialogContent>
                        <DialogActions>
                            <Button onClick={handleCloseDialog} disabled={leaveTypeLoading}>Cancel</Button>
                            <Button
                                variant="contained"
                                type="submit"
                                form="add-leave-type-form"
                                disabled={leaveTypeLoading}
                            >
                                {leaveTypeLoading ? <CircularProgress size={20} /> : 'Add'}
                            </Button>
                        </DialogActions>
                    </Dialog>
                    <Dialog open={openDialog === 'holiday'} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
                        <DialogTitle>Add Holiday</DialogTitle>
                        <DialogContent>
                            <form onSubmit={handleAddHoliday} id="add-holiday-form">
                                <TextField
                                    margin="normal"
                                    fullWidth
                                    label="Holiday Name"
                                    name="name"
                                    value={holidayForm.name}
                                    onChange={handleHolidayFormChange}
                                    required
                                />
                                <TextField
                                    margin="normal"
                                    fullWidth
                                    label="Date"
                                    name="date"
                                    type="date"
                                    value={holidayForm.date}
                                    onChange={handleHolidayFormChange}
                                    required
                                    InputLabelProps={{
                                        shrink: true,
                                    }}
                                />
                                <TextField
                                    margin="normal"
                                    fullWidth
                                    label="Description"
                                    name="description"
                                    value={holidayForm.description}
                                    onChange={handleHolidayFormChange}
                                    multiline
                                    rows={3}
                                />
                                <FormControl margin="normal" fullWidth>
                                    <FormControlLabel
                                        control={
                                            <Switch
                                                checked={holidayForm.isPublic}
                                                onChange={handleHolidayFormChange}
                                                name="isPublic"
                                            />
                                        }
                                        label="Public Holiday"
                                    />
                                </FormControl>
                                {holidayError && <Alert severity="error" sx={{ mt: 2 }}>{holidayError}</Alert>}
                            </form>
                        </DialogContent>
                        <DialogActions>
                            <Button onClick={handleCloseDialog} disabled={holidayLoading}>Cancel</Button>
                            <Button
                                variant="contained"
                                type="submit"
                                form="add-holiday-form"
                                disabled={holidayLoading}
                            >
                                {holidayLoading ? <CircularProgress size={20} /> : 'Add'}
                            </Button>
                        </DialogActions>
                    </Dialog>
                    <Snackbar
                        open={userSuccess}
                        autoHideDuration={3000}
                        onClose={() => setUserSuccess(false)}
                        message="User added successfully!"
                    />
                    <Snackbar
                        open={deptSuccess}
                        autoHideDuration={3000}
                        onClose={() => setDeptSuccess(false)}
                        message="Department added successfully!"
                    />
                    <Snackbar
                        open={leaveTypeSuccess}
                        autoHideDuration={3000}
                        onClose={() => setLeaveTypeSuccess(false)}
                        message="Leave type added successfully!"
                    />
                    <Snackbar
                        open={holidaySuccess}
                        autoHideDuration={3000}
                        onClose={() => setHolidaySuccess(false)}
                        message="Holiday added successfully!"
                    />
                    <Snackbar
                        open={snackbar.open}
                        autoHideDuration={3000}
                        onClose={() => setSnackbar({ ...snackbar, open: false })}
                        message={snackbar.message}
                    />
                    {/* Holiday Details Dialog */}
                    <Dialog open={holidayDialogOpen} onClose={() => setHolidayDialogOpen(false)}>
                        <DialogTitle>Holiday Details</DialogTitle>
                        <DialogContent>
                            {selectedHoliday && (
                                <>
                                    <Typography variant="h6">{selectedHoliday.title}</Typography>
                                    <Typography>Date: {dayjs(selectedHoliday.start).format('MMM D, YYYY')}</Typography>
                                    <Typography>Type: {selectedHoliday.isPublic ? 'Public' : 'Optional'}</Typography>
                                    {selectedHoliday.description && <Typography>Description: {selectedHoliday.description}</Typography>}
                                </>
                            )}
                        </DialogContent>
                    </Dialog>
                </Box>
            </Box>
        </Box>
    );
};

export default AdminDashboard; 