import React, { useEffect, useState, useRef } from 'react';
import Sidebar from '../../components/Sidebar';
import Topbar from '../../components/Topbar';
import { Box, Typography, Paper, Grid, Card, CardContent, CircularProgress, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Button, Dialog, DialogTitle, DialogContent, DialogActions, TextField, Chip, Alert, Avatar, Tooltip, IconButton } from '@mui/material';
import { authApi, leaveApi } from '@/lib/api';
import { useAuth } from '../../contexts/AuthContext';
import dayjs from 'dayjs';
import WarningIcon from '@mui/icons-material/Warning';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import GroupIcon from '@mui/icons-material/Group';
import AssignmentIcon from '@mui/icons-material/Assignment';
import EventBusyIcon from '@mui/icons-material/EventBusy';
import EventAvailableIcon from '@mui/icons-material/EventAvailable';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip as RechartsTooltip, Legend } from 'recharts';
import PersonIcon from '@mui/icons-material/Person';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid } from 'recharts';
import DescriptionIcon from '@mui/icons-material/Description';
import EditIcon from '@mui/icons-material/Edit';
import CheckBoxIcon from '@mui/icons-material/CheckBox';
import CheckBoxOutlineBlankIcon from '@mui/icons-material/CheckBoxOutlineBlank';
import VisibilityIcon from '@mui/icons-material/Visibility';
import DashboardIcon from '@mui/icons-material/Dashboard';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import EventNoteIcon from '@mui/icons-material/EventNote';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';

const managerNavItems = [
    { label: 'Dashboard', icon: <DashboardIcon />, path: '/manager/dashboard' },
    { label: 'Leave Approvals', icon: <AssignmentIcon />, path: '/manager/approvals' },
    { label: 'Team Calendar', icon: <CalendarMonthIcon />, path: '/manager/team-calendar' },
    { label: 'My Leave', icon: <EventNoteIcon />, path: '/manager/my-leave' },
    { label: 'Documents', icon: <AssignmentIcon />, path: '/manager/documents' },
    { label: 'Reports', icon: <AssignmentIcon />, path: '/manager/reports' },
    { label: 'Profile', icon: <AccountCircleIcon />, path: '/manager/profile' },
];

const ManagerDashboard = () => {
    const { user, setUser } = useAuth();
    const [stats, setStats] = useState({
        pendingApprovals: null,
        onLeaveToday: null,
        upcomingLeaves: null,
        departmentsManaged: null,
        lowBalanceAlerts: null,
        monthlyAccrual: null,
        leaveTypeDistribution: [],
        departmentDistribution: [],
        loading: true,
        error: ''
    });
    const [pendingApprovalsData, setPendingApprovalsData] = useState([]);
    const [loadingApprovals, setLoadingApprovals] = useState(true);
    const [approvalError, setApprovalError] = useState('');
    const [actionDialog, setActionDialog] = useState({ open: false, app: null, action: '' });
    const [actionComment, setActionComment] = useState('');
    const [actionLoading, setActionLoading] = useState(false);
    const [teamOnLeave, setTeamOnLeave] = useState([]);
    const [teamMembers, setTeamMembers] = useState([]);
    const [teamBalances, setTeamBalances] = useState({});
    const [teamLeaveStatus, setTeamLeaveStatus] = useState({});
    const [loadingTeam, setLoadingTeam] = useState(true);
    const [monthlyAnalytics, setMonthlyAnalytics] = useState([]);
    const [deptAnalytics, setDeptAnalytics] = useState([]);
    const [typeAnalytics, setTypeAnalytics] = useState([]);
    const [loadingAnalytics, setLoadingAnalytics] = useState(true);
    const [analyticsError, setAnalyticsError] = useState('');
    const [approvalDocuments, setApprovalDocuments] = useState({});
    const [adjustDialog, setAdjustDialog] = useState({ open: false, member: null });
    const [adjustType, setAdjustType] = useState('');
    const [adjustAmount, setAdjustAmount] = useState('');
    const [adjustReason, setAdjustReason] = useState('');
    const [adjustLoading, setAdjustLoading] = useState(false);
    const [adjustError, setAdjustError] = useState('');
    const [adjustSuccess, setAdjustSuccess] = useState('');
    const [leaveTypes, setLeaveTypes] = useState([]);
    const [selectedApprovals, setSelectedApprovals] = useState([]);
    const [bulkDialog, setBulkDialog] = useState({ open: false, action: '', comment: '' });
    const [bulkLoading, setBulkLoading] = useState(false);
    const [bulkError, setBulkError] = useState('');
    const [bulkSuccess, setBulkSuccess] = useState('');
    const [profileDialog, setProfileDialog] = useState({ open: false, member: null, balances: [] });
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [notifAnchorEl, setNotifAnchorEl] = useState(null);
    const [notifLoading, setNotifLoading] = useState(false);
    const [avatarUrl, setAvatarUrl] = useState('');
    const [managerDepartments, setManagerDepartments] = useState([]);
    const lastGoodTeamMembers = useRef([]);

    // Fetch profile and notifications ONCE on mount
    useEffect(() => {
        fetchNotifications();
        fetchProfile();
    }, []); // <-- empty dependency array

    // Fetch stats when user changes
    useEffect(() => {
        if (!user) return;
        const fetchStats = async () => {
            setStats(s => ({ ...s, loading: true, error: '' }));
            try {
                // 1. Fetch departments managed by this manager
                const deptRes = await authApi.get(`/departments/by-manager/${user.id}`);
                const departments = deptRes.data;
                const departmentsManaged = departments.length;

                // 2. Fetch all users in these departments
                let teamMembers = [];
                for (const dept of departments) {
                    if (dept.users) {
                        teamMembers = teamMembers.concat(dept.users);
                    }
                }
                const uniqueTeamMembers = Array.from(new Map(teamMembers.map(u => [u.id, u])).values());
                const teamMemberIds = uniqueTeamMembers.map(u => u.id);

                // 3. Fetch leave applications and balances
                let leaveApps = [];
                let leaveBalances = [];
                if (teamMemberIds.length > 0) {
                    const [leaveRes, balanceRes] = await Promise.all([
                        leaveApi.get(`/leaves`, {
                            params: { userIds: teamMemberIds.join(',') }
                        }),
                        leaveApi.get(`/leave-balances/bulk`, {
                            params: { userIds: teamMemberIds.join(',') }
                        })
                    ]);
                    leaveApps = leaveRes.data;
                    // Flatten the leaveBalances map into a single array for stats calculations
                    const balancesMap = balanceRes.data;
                    leaveBalances = Object.values(balancesMap).flat();
                }

                // 4. Compute enhanced stats
                const today = dayjs().format('YYYY-MM-DD');
                const nextWeek = dayjs().add(7, 'day').format('YYYY-MM-DD');
                const currentMonth = dayjs().month();

                // Basic stats
                const pendingApprovals = leaveApps.filter(app => app.status === 'PENDING').length;
                const onLeaveToday = leaveApps.filter(app =>
                    app.status === 'APPROVED' &&
                    dayjs(today).isBetween(app.startDate, app.endDate, null, '[]')
                ).length;
                const upcomingLeaves = leaveApps.filter(app =>
                    app.status === 'APPROVED' &&
                    dayjs(app.startDate).isAfter(today) &&
                    dayjs(app.startDate).isBefore(nextWeek)
                ).length;

                // Low balance alerts (less than 5 days remaining, for any leave type)
                const lowBalanceAlerts = leaveBalances.filter(bal =>
                    bal.remainingDays < 5
                ).length;

                // Monthly accrual status (sum for all leave types)
                const currentMonthAccrual = leaveBalances
                    .reduce((acc, bal) => acc + (bal.accruedDays || 0), 0);

                // Leave type distribution (dynamic)
                const leaveTypeDistribution = Object.entries(
                    leaveApps.reduce((acc, app) => {
                        const typeName = app.leaveType?.name || app.leaveTypeName || 'Unknown';
                        acc[typeName] = (acc[typeName] || 0) + 1;
                        return acc;
                    }, {})
                ).map(([name, value]) => ({ name, value }));

                // Department distribution (dynamic)
                const departmentDistribution = Object.entries(
                    teamMembers.reduce((acc, member) => {
                        const deptName = member.department?.name || member.department || 'Unassigned';
                        acc[deptName] = (acc[deptName] || 0) + 1;
                        return acc;
                    }, {})
                ).map(([name, value]) => ({ name, value }));

                setStats({
                    pendingApprovals,
                    onLeaveToday,
                    upcomingLeaves,
                    departmentsManaged,
                    lowBalanceAlerts,
                    monthlyAccrual: currentMonthAccrual,
                    leaveTypeDistribution,
                    departmentDistribution,
                    loading: false,
                    error: ''
                });
            } catch (err) {
                setStats(s => ({ ...s, loading: false, error: 'Failed to load stats' }));
            }
        };
        fetchStats();
    }, [user]);

    // Fetch pending approvals for manager's team
    useEffect(() => {
        const fetchPendingApprovals = async () => {
            if (!user) return;
            setLoadingApprovals(true);
            setApprovalError('');
            try {
                // 1. Fetch departments managed by this manager
                const deptRes = await authApi.get(`/departments/by-manager/${user.id}`);
                const departments = deptRes.data;
                let teamMembers = [];
                for (const dept of departments) {
                    if (dept.users) {
                        teamMembers = teamMembers.concat(dept.users);
                    }
                }
                const uniqueTeamMembers = Array.from(new Map(teamMembers.map(u => [u.id, u])).values());
                const teamMemberIds = uniqueTeamMembers.map(u => u.id);
                let leaveApps = [];
                if (teamMemberIds.length > 0) {
                    const leaveRes = await leaveApi.get(`/leaves`, {
                        params: { userIds: teamMemberIds.join(',') }
                    });
                    leaveApps = leaveRes.data;
                }
                // Only pending
                const pending = leaveApps.filter(app => app.status === 'PENDING');
                setPendingApprovalsData(pending);
            } catch (err) {
                setApprovalError('Failed to load pending approvals');
            } finally {
                setLoadingApprovals(false);
            }
        };
        fetchPendingApprovals();
    }, [user]);

    // Fetch team on leave today (reuse logic from stats)
    useEffect(() => {
        const fetchTeamOnLeave = async () => {
            if (!user) return;
            try {
                // 1. Fetch departments managed by this manager
                const deptRes = await authApi.get(`/departments/by-manager/${user.id}`);
                const departments = deptRes.data;
                let teamMembers = [];
                for (const dept of departments) {
                    if (dept.users) {
                        teamMembers = teamMembers.concat(dept.users);
                    }
                }
                const uniqueTeamMembers = Array.from(new Map(teamMembers.map(u => [u.id, u])).values());
                const teamMemberIds = uniqueTeamMembers.map(u => u.id);
                let leaveApps = [];
                if (teamMemberIds.length > 0) {
                    const leaveRes = await leaveApi.get(`/leaves`, {
                        params: { userIds: teamMemberIds.join(',') }
                    });
                    leaveApps = leaveRes.data;
                }
                const today = dayjs().format('YYYY-MM-DD');
                // On leave today (approved, today between start and end)
                const onLeave = leaveApps.filter(app =>
                    app.status === 'APPROVED' &&
                    dayjs(today).isBetween(app.startDate, app.endDate, null, '[]')
                );
                setTeamOnLeave(onLeave);
            } catch (err) {
                setTeamOnLeave([]);
            }
        };
        fetchTeamOnLeave();
    }, [user]);

    // Fetch team members, their balances, and leave status
    useEffect(() => {
        if (!user || !user.id) return;
        setLoadingTeam(true);
        let isMounted = true;
        const fetchTeamData = async () => {
            try {
                // 1. Fetch departments managed by this manager
                const deptRes = await authApi.get(`/departments/by-manager/${user.id}`);
                const departments = deptRes.data;
                let members = [];
                for (const dept of departments) {
                    if (dept.users) {
                        members = members.concat(dept.users.map(u => ({ ...u, department: dept.name })));
                    }
                }
                // Remove duplicates by user id
                const uniqueMembers = Array.from(new Map(members.map(u => [u.id, u])).values());
                if (uniqueMembers.length > 0) {
                    lastGoodTeamMembers.current = uniqueMembers;
                    if (isMounted) setTeamMembers(uniqueMembers);
                } else if (isMounted) {
                    setTeamMembers(lastGoodTeamMembers.current);
                }
                const memberIds = uniqueMembers.map(u => u.id);
                // 2. Fetch leave balances for all team members
                let balances = {};
                if (memberIds.length > 0) {
                    const balanceRes = await leaveApi.get(`/leave-balances/bulk`, {
                        params: { userIds: memberIds.join(',') }
                    });
                    balances = balanceRes.data; // { userId: [LeaveBalance, ...] }
                }
                if (isMounted) setTeamBalances(balances);
                // 3. Fetch leave applications for all team members (to determine current leave status)
                let leaveStatus = {};
                if (memberIds.length > 0) {
                    const leaveRes = await leaveApi.get(`/leaves`, {
                        params: { userIds: memberIds.join(',') }
                    });
                    const today = dayjs().format('YYYY-MM-DD');
                    for (const app of leaveRes.data) {
                        if (
                            app.status === 'APPROVED' &&
                            dayjs(today).isBetween(app.startDate, app.endDate, null, '[]')
                        ) {
                            leaveStatus[app.user.id] = {
                                status: 'On Leave',
                                leaveType: app.leaveType.name,
                                until: app.endDate
                            };
                        }
                    }
                }
                if (isMounted) setTeamLeaveStatus(leaveStatus);
            } catch (err) {
                // Do not clear teamMembers on error
                if (isMounted) {
                    setTeamBalances({});
                    setTeamLeaveStatus({});
                }
            } finally {
                if (isMounted) setLoadingTeam(false);
            }
        };
        fetchTeamData();
        return () => { isMounted = false; };
    }, [user]);

    // Fetch manager's departments once for analytics
    useEffect(() => {
        if (!user) return;
        const fetchDepartments = async () => {
            try {
                const deptRes = await authApi.get(`/departments/by-manager/${user.id}`);
                setManagerDepartments(deptRes.data);
            } catch {
                setManagerDepartments([]);
            }
        };
        fetchDepartments();
    }, [user]);

    // Fetch analytics from backend endpoints
    useEffect(() => {
        if (!user || !managerDepartments || managerDepartments.length === 0) {
            setDeptAnalytics([]);
            setTypeAnalytics([]);
            setMonthlyAnalytics([]);
            setLoadingAnalytics(false);
            return;
        }
        setLoadingAnalytics(true);
        setAnalyticsError('');
        const departmentIds = managerDepartments.map(d => d.id);
        const year = new Date().getFullYear();
        Promise.all([
            // Department-wise
            Promise.all(departmentIds.map(id => leaveApi.get(`/leave-analytics/department-distribution?year=${year}&departmentId=${id}`))),
            // Usage trends (monthly)
            leaveApi.get(`/leave-analytics/usage-trends?year=${year}&interval=MONTHLY`),
            // YTD consumption (leave type)
            leaveApi.get(`/leave-analytics/ytd-consumption?year=${year}`)
        ]).then(([deptResArr, usageRes, ytdRes]) => {
            // Merge department analytics
            const deptData = deptResArr.flatMap(res => Object.entries(res.data).map(([dept, stats]) => ({
                department: dept,
                ...stats
            })));
            setDeptAnalytics(deptData);
            // Monthly usage
            const usageData = Object.entries(usageRes.data).map(([month, leaves]) => ({
                month,
                leaves
            }));
            setMonthlyAnalytics(usageData);
            // Leave type usage
            const typeData = Object.entries(ytdRes.data).map(([type, leaves]) => ({
                leaveType: type,
                leaves
            }));
            setTypeAnalytics(typeData);
        }).catch(() => {
            setAnalyticsError('Failed to load analytics data');
            setDeptAnalytics([]);
            setTypeAnalytics([]);
            setMonthlyAnalytics([]);
        }).finally(() => setLoadingAnalytics(false));
    }, [user, managerDepartments]);

    // Fetch documents for pending approvals
    useEffect(() => {
        const fetchDocuments = async () => {
            if (!pendingApprovalsData.length) return;
            const docsMap = {};
            await Promise.all(pendingApprovalsData.map(async (app) => {
                let docs = [];
                // Prefer the 'documents' array if present (already contains metadata)
                if (app.documents && Array.isArray(app.documents) && app.documents.length > 0) {
                    docs = app.documents;
                } else if (app.documentIds && Array.isArray(app.documentIds) && app.documentIds.length > 0) {
                    // Otherwise, fetch metadata for each documentId
                    await Promise.all(app.documentIds.map(async (docId) => {
                        try {
                            const res = await leaveApi.get(`/documents/${docId}`);
                            docs.push(res.data);
                        } catch { }
                    }));
                }
                docsMap[app.id] = docs;
            }));
            setApprovalDocuments(docsMap);
        };
        fetchDocuments();
    }, [pendingApprovalsData]);

    // Fetch leave types for adjustment dialog
    useEffect(() => {
        const fetchTypes = async () => {
            try {
                const res = await leaveApi.get(`/leave-types`);
                setLeaveTypes(res.data);
            } catch { }
        };
        fetchTypes();
    }, []);

    // Approve/Reject handlers
    const handleAction = (app, action) => {
        setActionDialog({ open: true, app, action });
        setActionComment('');
    };
    const handleActionClose = () => {
        setActionDialog({ open: false, app: null, action: '' });
    };
    const handleActionSubmit = async () => {
        setActionLoading(true);
        try {
            const url = `/leaves/${actionDialog.app.id}/${actionDialog.action}`;
            await leaveApi.post(url, { comments: actionComment });
            handleActionClose();
            // Refresh pending approvals
            setTimeout(() => {
                window.location.reload();
            }, 500);
        } catch (err) {
            alert('Failed to process action');
        } finally {
            setActionLoading(false);
        }
    };

    const handleOpenAdjust = (member) => {
        setAdjustDialog({ open: true, member });
        setAdjustType('');
        setAdjustAmount('');
        setAdjustReason('');
        setAdjustError('');
        setAdjustSuccess('');
    };
    const handleCloseAdjust = () => {
        setAdjustDialog({ open: false, member: null });
    };
    const handleSubmitAdjust = async (e) => {
        e.preventDefault();
        setAdjustLoading(true);
        setAdjustError('');
        setAdjustSuccess('');
        try {
            await leaveApi.post('/leave-balances/adjust', {
                userId: adjustDialog.member.id,
                leaveTypeId: adjustType,
                adjustmentDays: Number(adjustAmount),
                reason: adjustReason
            });
            setAdjustSuccess('Balance adjusted successfully.');
            setTimeout(() => {
                handleCloseAdjust();
                window.location.reload();
            }, 1000);
        } catch (err) {
            setAdjustError('Failed to adjust balance.');
        } finally {
            setAdjustLoading(false);
        }
    };

    const handleSelectApproval = (id) => {
        setSelectedApprovals((prev) =>
            prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
        );
    };
    const handleSelectAllApprovals = () => {
        if (selectedApprovals.length === pendingApprovalsData.length) {
            setSelectedApprovals([]);
        } else {
            setSelectedApprovals(pendingApprovalsData.map((app) => app.id));
        }
    };
    const handleOpenBulkDialog = (action) => {
        setBulkDialog({ open: true, action, comment: '' });
        setBulkError('');
        setBulkSuccess('');
    };
    const handleCloseBulkDialog = () => {
        setBulkDialog({ open: false, action: '', comment: '' });
    };
    const handleBulkAction = async (e) => {
        e.preventDefault();
        setBulkLoading(true);
        setBulkError('');
        setBulkSuccess('');
        try {
            await Promise.all(selectedApprovals.map((id) =>
                leaveApi.post(`/leaves/${id}/${bulkDialog.action}`, { comments: bulkDialog.comment })
            ));
            setBulkSuccess('Bulk action completed.');
            setTimeout(() => {
                handleCloseBulkDialog();
                setSelectedApprovals([]);
                window.location.reload();
            }, 1000);
        } catch {
            setBulkError('Bulk action failed.');
        } finally {
            setBulkLoading(false);
        }
    };

    const handleOpenProfile = (member) => {
        // Find balances for this member
        const balances = teamBalances[String(member.id)] || [];
        setProfileDialog({ open: true, member, balances });
    };
    const handleCloseProfile = () => {
        setProfileDialog({ open: false, member: null, balances: [] });
    };

    const fetchNotifications = async () => {
        setNotifLoading(true);
        try {
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

    const fetchProfile = async () => {
        try {
            const res = await authApi.get('/profile');
            setUser(res.data);
            setAvatarUrl(res.data.avatarUrl || '');
        } catch {
            setUser(null);
            setAvatarUrl('');
        }
    };

    const handleNotificationsClick = (e) => setNotifAnchorEl(e.currentTarget);
    const handleCloseNotifications = () => setNotifAnchorEl(null);
    const handleMarkAllRead = async () => {
        await leaveApi.post('/notifications/mark-all-read');
        fetchNotifications();
    };
    const handleMarkRead = async (id) => {
        await leaveApi.post(`/notifications/${id}/mark-read`);
        fetchNotifications();
    };

    const handleApprovalDocDownload = async (docId, fileName) => {
        try {
            const res = await leaveApi.get(`/documents/download/${fileName}`, { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([res.data]));
            const newWindow = window.open(url, '_blank');
            if (!newWindow) {
                const link = document.createElement('a');
                link.href = url;
                link.setAttribute('download', fileName);
                document.body.appendChild(link);
                link.click();
                link.remove();
            }
            setTimeout(() => window.URL.revokeObjectURL(url), 1000);
        } catch (err) {
            alert('Failed to download document');
        }
    };

    const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884D8'];

    return (
        <Box sx={{ display: 'flex', minHeight: '100vh', background: '#f7f7f7' }}>
            <Sidebar items={managerNavItems} />
            <Box sx={{ flexGrow: 1 }}>
                <Topbar
                    title="Manager Dashboard"
                    avatarLetter={user && user.avatarUrl ? undefined : (user ? user.firstName?.[0] : 'M')}
                    avatarUrl={avatarUrl}
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
                        Welcome, {user?.lastName || 'Manager'}
                    </Typography>

                    {/* Enhanced Quick Stats */}
                    <Paper sx={{ p: 3, mb: 2 }}>
                        <Typography variant="h6" sx={{ mb: 2 }}>Quick Stats</Typography>
                        {stats.loading ? (
                            <Box display="flex" justifyContent="center" alignItems="center" minHeight="100px">
                                <CircularProgress />
                            </Box>
                        ) : stats.error ? (
                            <Typography color="error">{stats.error}</Typography>
                        ) : (
                            <Grid container spacing={3}>
                                <Grid item xs={12} sm={6} md={3}>
                                    <Card>
                                        <CardContent>
                                            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                                                <AssignmentIcon color="primary" sx={{ mr: 1 }} />
                                                <Typography color="text.secondary">
                                                    Pending Approvals
                                                </Typography>
                                            </Box>
                                            <Typography variant="h4">
                                                {stats.pendingApprovals ?? 0}
                                            </Typography>
                                        </CardContent>
                                    </Card>
                                </Grid>
                                <Grid item xs={12} sm={6} md={3}>
                                    <Card>
                                        <CardContent>
                                            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                                                <EventBusyIcon color="error" sx={{ mr: 1 }} />
                                                <Typography color="text.secondary">
                                                    On Leave Today
                                                </Typography>
                                            </Box>
                                            <Typography variant="h4">
                                                {stats.onLeaveToday ?? 0}
                                            </Typography>
                                        </CardContent>
                                    </Card>
                                </Grid>
                                <Grid item xs={12} sm={6} md={3}>
                                    <Card>
                                        <CardContent>
                                            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                                                <EventAvailableIcon color="success" sx={{ mr: 1 }} />
                                                <Typography color="text.secondary">
                                                    Upcoming Leaves
                                                </Typography>
                                            </Box>
                                            <Typography variant="h4">
                                                {stats.upcomingLeaves ?? 0}
                                            </Typography>
                                        </CardContent>
                                    </Card>
                                </Grid>
                                <Grid item xs={12} sm={6} md={3}>
                                    <Card>
                                        <CardContent>
                                            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                                                <GroupIcon color="info" sx={{ mr: 1 }} />
                                                <Typography color="text.secondary">
                                                    Departments
                                                </Typography>
                                            </Box>
                                            <Typography variant="h4">
                                                {stats.departmentsManaged ?? 0}
                                            </Typography>
                                        </CardContent>
                                    </Card>
                                </Grid>
                                <Grid item xs={12} sm={6} md={3}>
                                    <Card>
                                        <CardContent>
                                            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                                                <WarningIcon color="warning" sx={{ mr: 1 }} />
                                                <Typography color="text.secondary">
                                                    Low Balance Alerts
                                                </Typography>
                                            </Box>
                                            <Typography variant="h4">
                                                {stats.lowBalanceAlerts ?? 0}
                                            </Typography>
                                        </CardContent>
                                    </Card>
                                </Grid>
                                <Grid item xs={12} sm={6} md={3}>
                                    <Card>
                                        <CardContent>
                                            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                                                <TrendingUpIcon color="success" sx={{ mr: 1 }} />
                                                <Typography color="text.secondary">
                                                    Monthly Accrual
                                                </Typography>
                                            </Box>
                                            <Typography variant="h4">
                                                {stats.monthlyAccrual?.toFixed(2) ?? '0.00'} days
                                            </Typography>
                                            <Typography variant="caption" color="text.secondary">
                                                Target: 1.66 days/month
                                            </Typography>
                                        </CardContent>
                                    </Card>
                                </Grid>
                            </Grid>
                        )}
                    </Paper>

                    {/* Leave Distribution Charts */}
                    <Grid container spacing={2} sx={{ mb: 2 }}>
                        <Grid item xs={12} md={6}>
                            <Paper sx={{ p: 3 }}>
                                <Typography variant="h6" sx={{ mb: 2 }}>Leave Type Distribution</Typography>
                                <Box sx={{ height: 300 }}>
                                    {stats.loading ? (
                                        <Box display="flex" alignItems="center" justifyContent="center" height="100%">
                                            <CircularProgress />
                                        </Box>
                                    ) : stats.leaveTypeDistribution.length === 0 ? (
                                        <Box display="flex" alignItems="center" justifyContent="center" height="100%">
                                            <Typography color="text.secondary">No leave type data available</Typography>
                                        </Box>
                                    ) : (
                                        <ResponsiveContainer width="100%" height="100%">
                                            <PieChart>
                                                <Pie
                                                    data={stats.leaveTypeDistribution}
                                                    dataKey="value"
                                                    nameKey="name"
                                                    cx="50%"
                                                    cy="50%"
                                                    outerRadius={80}
                                                    label
                                                >
                                                    {stats.leaveTypeDistribution.map((entry, index) => (
                                                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                                    ))}
                                                </Pie>
                                                <RechartsTooltip />
                                                <Legend />
                                            </PieChart>
                                        </ResponsiveContainer>
                                    )}
                                </Box>
                            </Paper>
                        </Grid>
                        <Grid item xs={12} md={6}>
                            <Paper sx={{ p: 3 }}>
                                <Typography variant="h6" sx={{ mb: 2 }}>Department Distribution</Typography>
                                <Box sx={{ height: 300 }}>
                                    {stats.loading ? (
                                        <Box display="flex" alignItems="center" justifyContent="center" height="100%">
                                            <CircularProgress />
                                        </Box>
                                    ) : stats.departmentDistribution.length === 0 ? (
                                        <Box display="flex" alignItems="center" justifyContent="center" height="100%">
                                            <Typography color="text.secondary">No department data available</Typography>
                                        </Box>
                                    ) : (
                                        <ResponsiveContainer width="100%" height="100%">
                                            <PieChart>
                                                <Pie
                                                    data={stats.departmentDistribution}
                                                    dataKey="value"
                                                    nameKey="name"
                                                    cx="50%"
                                                    cy="50%"
                                                    outerRadius={80}
                                                    label
                                                >
                                                    {stats.departmentDistribution.map((entry, index) => (
                                                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                                    ))}
                                                </Pie>
                                                <RechartsTooltip />
                                                <Legend />
                                            </PieChart>
                                        </ResponsiveContainer>
                                    )}
                                </Box>
                            </Paper>
                        </Grid>
                    </Grid>

                    <Paper sx={{ p: 3, mb: 2 }}>
                        <Typography variant="h6">Team on Leave</Typography>
                        {teamOnLeave.length === 0 ? (
                            <Alert severity="info">No team members are on leave today</Alert>
                        ) : (
                            <TableContainer>
                                <Table size="small">
                                    <TableHead>
                                        <TableRow>
                                            <TableCell>Employee</TableCell>
                                            <TableCell>Leave Type</TableCell>
                                            <TableCell>Return Date</TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {teamOnLeave.map((app) => (
                                            <TableRow key={app.id}>
                                                <TableCell>{app.employeeName}</TableCell>
                                                <TableCell>{app.leaveType}</TableCell>
                                                <TableCell>{app.endDate}</TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                        )}
                    </Paper>
                    <Paper sx={{ p: 3, mb: 2 }}>
                        <Typography variant="h6">Pending Approvals</Typography>
                        {loadingApprovals ? (
                            <Box display="flex" justifyContent="center" p={3}>
                                <CircularProgress />
                            </Box>
                        ) : approvalError ? (
                            <Alert severity="error">{approvalError}</Alert>
                        ) : pendingApprovalsData.length === 0 ? (
                            <Alert severity="info">No pending approvals</Alert>
                        ) : (
                            <Box display="flex" alignItems="center" gap={2} mb={2}>
                                <Button
                                    variant="contained"
                                    color="success"
                                    disabled={selectedApprovals.length === 0}
                                    onClick={() => handleOpenBulkDialog('approve')}
                                >
                                    Approve Selected
                                </Button>
                                <Button
                                    variant="contained"
                                    color="error"
                                    disabled={selectedApprovals.length === 0}
                                    onClick={() => handleOpenBulkDialog('reject')}
                                >
                                    Reject Selected
                                </Button>
                                <Typography variant="body2" color="text.secondary">
                                    {selectedApprovals.length} selected
                                </Typography>
                            </Box>
                        )}
                        <TableContainer>
                            <Table size="small">
                                <TableHead>
                                    <TableRow>
                                        <TableCell padding="checkbox">
                                            <IconButton onClick={handleSelectAllApprovals} size="small">
                                                {selectedApprovals.length === pendingApprovalsData.length && pendingApprovalsData.length > 0 ? <CheckBoxIcon /> : <CheckBoxOutlineBlankIcon />}
                                            </IconButton>
                                        </TableCell>
                                        <TableCell>Employee</TableCell>
                                        <TableCell>Leave Type</TableCell>
                                        <TableCell>Start Date</TableCell>
                                        <TableCell>End Date</TableCell>
                                        <TableCell>Days</TableCell>
                                        <TableCell>Status</TableCell>
                                        <TableCell>Document</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {pendingApprovalsData.map((app) => (
                                        <TableRow key={app.id}>
                                            <TableCell padding="checkbox">
                                                <IconButton onClick={() => handleSelectApproval(app.id)} size="small">
                                                    {selectedApprovals.includes(app.id) ? <CheckBoxIcon color="primary" /> : <CheckBoxOutlineBlankIcon />}
                                                </IconButton>
                                            </TableCell>
                                            <TableCell>{app.employeeName}</TableCell>
                                            <TableCell>{app.leaveType}</TableCell>
                                            <TableCell>{app.startDate}</TableCell>
                                            <TableCell>{app.endDate}</TableCell>
                                            <TableCell>{app.totalDays}</TableCell>
                                            <TableCell>
                                                <Chip label={app.status} color="warning" size="small" />
                                            </TableCell>
                                            <TableCell>
                                                {approvalDocuments[app.id] && approvalDocuments[app.id].length > 0 ? (
                                                    <IconButton
                                                        onClick={() => handleApprovalDocDownload(approvalDocuments[app.id][0].id, approvalDocuments[app.id][0].fileName)}
                                                        title="View/Download Document"
                                                    >
                                                        <DescriptionIcon color="primary" />
                                                    </IconButton>
                                                ) : (
                                                    <Typography variant="caption" color="text.secondary">None</Typography>
                                                )}
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </TableContainer>
                        {/* Action Dialog */}
                        <Dialog open={actionDialog.open} onClose={handleActionClose} maxWidth="sm" fullWidth>
                            <DialogTitle>{actionDialog.action === 'approve' ? 'Approve' : 'Reject'} Leave Application</DialogTitle>
                            <DialogContent>
                                <Typography>Employee: {actionDialog.app?.employeeName}</Typography>
                                <Typography>Leave Type: {actionDialog.app?.leaveType?.name}</Typography>
                                <Typography>Dates: {actionDialog.app?.startDate} to {actionDialog.app?.endDate}</Typography>
                                <TextField
                                    margin="normal"
                                    fullWidth
                                    label="Comments"
                                    value={actionComment}
                                    onChange={e => setActionComment(e.target.value)}
                                    multiline
                                    rows={3}
                                />
                            </DialogContent>
                            <DialogActions>
                                <Button onClick={handleActionClose} disabled={actionLoading}>Cancel</Button>
                                <Button variant="contained" onClick={handleActionSubmit} disabled={actionLoading} color={actionDialog.action === 'approve' ? 'success' : 'error'}>
                                    {actionLoading ? <CircularProgress size={20} /> : actionDialog.action === 'approve' ? 'Approve' : 'Reject'}
                                </Button>
                            </DialogActions>
                        </Dialog>
                    </Paper>

                    <Paper sx={{ p: 3, mb: 2 }}>
                        <Typography variant="h6" sx={{ mb: 2 }}>Team Overview</Typography>
                        {loadingTeam ? (
                            <Box display="flex" justifyContent="center" alignItems="center" minHeight="100px">
                                <CircularProgress />
                            </Box>
                        ) : teamMembers.length === 0 ? (
                            <Alert severity="info">No team members found in your departments.</Alert>
                        ) : (
                            <>
                                <Grid container spacing={2}>
                                    {teamMembers.map(member => {
                                        const balances = teamBalances[String(member.id)] || [];
                                        const leave = teamLeaveStatus[member.id];
                                        return (
                                            <Grid item xs={12} sm={6} md={4} lg={3} key={member.id}>
                                                <Card>
                                                    <CardContent>
                                                        <Box display="flex" alignItems="center" mb={1}>
                                                            {member.avatarUrl ? (
                                                                <Avatar src={member.avatarUrl} sx={{ width: 48, height: 48, mr: 2 }} />
                                                            ) : (
                                                                <Avatar sx={{ width: 48, height: 48, mr: 2 }}><PersonIcon /></Avatar>
                                                            )}
                                                            <Box>
                                                                <Typography variant="subtitle1">{member.firstName} {member.lastName}</Typography>
                                                                <Typography variant="body2" color="text.secondary">{member.department}</Typography>
                                                            </Box>
                                                        </Box>
                                                        <Typography variant="body2" color={leave ? 'error' : 'success.main'}>
                                                            {leave ? `On Leave (${leave.leaveType}) until ${leave.until}` : 'Available'}
                                                        </Typography>
                                                        <Box mt={2}>
                                                            {balances.length === 0 ? (
                                                                <Typography variant="body2">No leave balances found.</Typography>
                                                            ) : (
                                                                balances.map(bal => (
                                                                    <Typography key={bal.leaveTypeId || bal.leaveType?.id || bal.leaveTypeName} variant="body2">
                                                                        {(bal.leaveTypeName || bal.leaveType?.name || 'Leave')}: {bal.remainingDays} / {bal.totalDays} days
                                                                        {typeof bal.accruedDays !== 'undefined' && ` (Accrued: ${bal.accruedDays})`}
                                                                        {typeof bal.carriedOverDays !== 'undefined' && ` (Carry-forward: ${bal.carriedOverDays})`}
                                                                    </Typography>
                                                                ))
                                                            )}
                                                        </Box>
                                                        <Box display="flex" alignItems="center" justifyContent="space-between" mt={2}>
                                                            <Box>
                                                                <IconButton size="small" onClick={() => handleOpenProfile(member)} title="View Profile">
                                                                    <VisibilityIcon fontSize="small" />
                                                                </IconButton>
                                                                <IconButton size="small" onClick={() => handleOpenAdjust(member)} title="Adjust Balance">
                                                                    <EditIcon fontSize="small" />
                                                                </IconButton>
                                                            </Box>
                                                        </Box>
                                                    </CardContent>
                                                </Card>
                                            </Grid>
                                        );
                                    })}
                                </Grid>
                            </>
                        )}
                    </Paper>

                    <Paper sx={{ p: 3, mb: 2 }}>
                        <Typography variant="h6" sx={{ mb: 2 }}>Leave Analytics</Typography>
                        {loadingAnalytics ? (
                            <Box display="flex" justifyContent="center" alignItems="center" minHeight="100px">
                                <CircularProgress />
                            </Box>
                        ) : analyticsError ? (
                            <Alert severity="error">{analyticsError}</Alert>
                        ) : monthlyAnalytics.length === 0 ? (
                            <Alert severity="info">No leave data available for analytics.</Alert>
                        ) : (
                            <BarChart width={600} height={300} data={monthlyAnalytics} margin={{ top: 20, right: 30, left: 0, bottom: 5 }}>
                                <CartesianGrid strokeDasharray="3 3" />
                                <XAxis dataKey="month" />
                                <YAxis allowDecimals={false} />
                                <RechartsTooltip />
                                <Bar dataKey="leaves" fill="#1976d2" />
                            </BarChart>
                        )}
                    </Paper>

                    <Grid container spacing={2}>
                        <Grid item xs={12} md={6}>
                            <Typography variant="subtitle1" sx={{ mb: 1 }}>Department-wise Leave Usage</Typography>
                            {loadingAnalytics ? (
                                <Box display="flex" justifyContent="center" alignItems="center" minHeight="100px">
                                    <CircularProgress />
                                </Box>
                            ) : deptAnalytics.length === 0 ? (
                                <Alert severity="info">No department leave data available.</Alert>
                            ) : (
                                <BarChart width={300} height={250} data={deptAnalytics} margin={{ top: 20, right: 30, left: 0, bottom: 5 }}>
                                    <CartesianGrid strokeDasharray="3 3" />
                                    <XAxis dataKey="department" />
                                    <YAxis allowDecimals={false} />
                                    <RechartsTooltip />
                                    <Bar dataKey="leaves" fill="#43a047" />
                                </BarChart>
                            )}
                        </Grid>
                        <Grid item xs={12} md={6}>
                            <Typography variant="subtitle1" sx={{ mb: 1 }}>Leave Type-wise Usage</Typography>
                            {loadingAnalytics ? (
                                <Box display="flex" justifyContent="center" alignItems="center" minHeight="100px">
                                    <CircularProgress />
                                </Box>
                            ) : typeAnalytics.length === 0 ? (
                                <Alert severity="info">No leave type data available.</Alert>
                            ) : (
                                <BarChart width={300} height={250} data={typeAnalytics} margin={{ top: 20, right: 30, left: 0, bottom: 5 }}>
                                    <CartesianGrid strokeDasharray="3 3" />
                                    <XAxis dataKey="leaveType" />
                                    <YAxis allowDecimals={false} />
                                    <RechartsTooltip />
                                    <Bar dataKey="leaves" fill="#fbc02d" />
                                </BarChart>
                            )}
                        </Grid>
                    </Grid>

                    {/* Adjust Balance Dialog */}
                    <Dialog open={adjustDialog.open} onClose={handleCloseAdjust} maxWidth="xs" fullWidth>
                        <DialogTitle>Adjust Leave Balance</DialogTitle>
                        <form onSubmit={handleSubmitAdjust}>
                            <DialogContent>
                                <Typography gutterBottom>
                                    {adjustDialog.member ? `${adjustDialog.member.firstName} ${adjustDialog.member.lastName}` : ''}
                                </Typography>
                                <TextField
                                    select
                                    label="Leave Type"
                                    value={adjustType}
                                    onChange={e => setAdjustType(e.target.value)}
                                    fullWidth
                                    margin="normal"
                                    required
                                    SelectProps={{ native: true }}
                                >
                                    <option value="" disabled>Select leave type</option>
                                    {leaveTypes.map(type => (
                                        <option key={type.id} value={type.id}>{type.name}</option>
                                    ))}
                                </TextField>
                                <TextField
                                    label="Adjustment Days"
                                    type="number"
                                    value={adjustAmount}
                                    onChange={e => setAdjustAmount(e.target.value)}
                                    fullWidth
                                    margin="normal"
                                    required
                                />
                                <TextField
                                    label="Reason"
                                    value={adjustReason}
                                    onChange={e => setAdjustReason(e.target.value)}
                                    fullWidth
                                    margin="normal"
                                    required
                                />
                                {adjustError && <Alert severity="error" sx={{ mt: 2 }}>{adjustError}</Alert>}
                                {adjustSuccess && <Alert severity="success" sx={{ mt: 2 }}>{adjustSuccess}</Alert>}
                            </DialogContent>
                            <DialogActions>
                                <Button onClick={handleCloseAdjust} disabled={adjustLoading}>Cancel</Button>
                                <Button type="submit" variant="contained" disabled={adjustLoading || !adjustType || !adjustAmount || !adjustReason}>
                                    {adjustLoading ? <CircularProgress size={20} /> : 'Adjust'}
                                </Button>
                            </DialogActions>
                        </form>
                    </Dialog>

                    {/* Bulk Action Dialog */}
                    <Dialog open={bulkDialog.open} onClose={handleCloseBulkDialog} maxWidth="xs" fullWidth>
                        <DialogTitle>{bulkDialog.action === 'approve' ? 'Approve' : 'Reject'} Selected Requests</DialogTitle>
                        <form onSubmit={handleBulkAction}>
                            <DialogContent>
                                <Typography gutterBottom>
                                    {selectedApprovals.length} requests will be {bulkDialog.action === 'approve' ? 'approved' : 'rejected'}.
                                </Typography>
                                <TextField
                                    label="Comment"
                                    value={bulkDialog.comment}
                                    onChange={e => setBulkDialog({ ...bulkDialog, comment: e.target.value })}
                                    fullWidth
                                    margin="normal"
                                    required
                                    multiline
                                    rows={3}
                                />
                                {bulkError && <Alert severity="error" sx={{ mt: 2 }}>{bulkError}</Alert>}
                                {bulkSuccess && <Alert severity="success" sx={{ mt: 2 }}>{bulkSuccess}</Alert>}
                            </DialogContent>
                            <DialogActions>
                                <Button onClick={handleCloseBulkDialog} disabled={bulkLoading}>Cancel</Button>
                                <Button type="submit" variant="contained" disabled={bulkLoading || !bulkDialog.comment}>
                                    {bulkLoading ? <CircularProgress size={20} /> : (bulkDialog.action === 'approve' ? 'Approve' : 'Reject')}
                                </Button>
                            </DialogActions>
                        </form>
                    </Dialog>

                    {/* Profile Dialog */}
                    <Dialog open={profileDialog.open} onClose={handleCloseProfile} maxWidth="sm" fullWidth>
                        <DialogTitle>Team Member Profile</DialogTitle>
                        <DialogContent>
                            {profileDialog.member && (
                                <Box>
                                    <Box display="flex" alignItems="center" mb={2}>
                                        {profileDialog.member.avatarUrl ? (
                                            <Avatar src={profileDialog.member.avatarUrl} sx={{ width: 56, height: 56, mr: 2 }} />
                                        ) : (
                                            <Avatar sx={{ width: 56, height: 56, mr: 2 }}><PersonIcon /></Avatar>
                                        )}
                                        <Box>
                                            <Typography variant="h6">{profileDialog.member.firstName} {profileDialog.member.lastName}</Typography>
                                            <Typography variant="body2" color="text.secondary">{profileDialog.member.email}</Typography>
                                            <Typography variant="body2" color="text.secondary">Department: {profileDialog.member.department}</Typography>
                                        </Box>
                                    </Box>
                                    <Typography variant="subtitle1" sx={{ mt: 2 }}>Leave Balances</Typography>
                                    {profileDialog.balances.length === 0 ? (
                                        <Typography variant="body2" color="text.secondary">No leave balances found.</Typography>
                                    ) : (
                                        <Box component="ul" sx={{ pl: 2 }}>
                                            {profileDialog.balances.map(bal => (
                                                <li key={bal.id}>
                                                    {(bal.leaveTypeName || bal.leaveType?.name || 'Leave')}: {bal.remainingDays} / {bal.totalDays} days (Used: {bal.usedDays})<br />
                                                    {typeof bal.accruedDays !== 'undefined' && (
                                                        <span>Accrued this month: {bal.accruedDays} days. </span>
                                                    )}
                                                    {typeof bal.carriedOverDays !== 'undefined' && (
                                                        <span>Carry-forward: {bal.carriedOverDays} days (expires Jan 31)</span>
                                                    )}
                                                </li>
                                            ))}
                                        </Box>
                                    )}
                                </Box>
                            )}
                        </DialogContent>
                        <DialogActions>
                            <Button onClick={handleCloseProfile}>Close</Button>
                        </DialogActions>
                    </Dialog>
                </Box>
            </Box>
        </Box>
    );
};

export default ManagerDashboard; 