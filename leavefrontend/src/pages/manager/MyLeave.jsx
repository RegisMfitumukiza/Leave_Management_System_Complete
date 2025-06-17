import React, { useEffect, useState } from 'react';
import { Box, Typography, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, CircularProgress, Alert, Dialog, DialogTitle, DialogContent, DialogActions, Button as MuiButton } from '@mui/material';
import { leaveApi, authApi } from '@/lib/api';
import { useAuth } from '../../contexts/AuthContext';
import dayjs from 'dayjs';

const MyLeave = () => {
    const { user } = useAuth();
    const [teamMembers, setTeamMembers] = useState([]);
    const [teamBalances, setTeamBalances] = useState({});
    const [loadingTeam, setLoadingTeam] = useState(true);
    const [teamError, setTeamError] = useState('');
    const [historyDialog, setHistoryDialog] = useState({ open: false, member: null });
    const [historyLoading, setHistoryLoading] = useState(false);
    const [historyError, setHistoryError] = useState('');
    const [historyData, setHistoryData] = useState([]);

    useEffect(() => {
        const fetchTeamData = async () => {
            setLoadingTeam(true);
            setTeamError('');
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
                setTeamMembers(uniqueMembers);
                const memberIds = uniqueMembers.map(u => u.id);
                // 2. Fetch leave balances for all team members
                let balances = {};
                if (memberIds.length > 0) {
                    const balanceRes = await leaveApi.get(`/leave-balances/bulk`, {
                        params: { userIds: memberIds.join(',') }
                    });
                    balances = balanceRes.data; // { userId: [LeaveBalance, ...] }
                }
                setTeamBalances(balances);
            } catch (err) {
                setTeamError('Failed to load team data.');
                setTeamMembers([]);
                setTeamBalances({});
            } finally {
                setLoadingTeam(false);
            }
        };
        if (user) fetchTeamData();
    }, [user]);

    const handleOpenHistory = async (member) => {
        setHistoryDialog({ open: true, member });
        setHistoryLoading(true);
        setHistoryError('');
        setHistoryData([]);
        try {
            const res = await leaveApi.get(`/leaves?userId=${member.id}`);
            setHistoryData(res.data);
        } catch (err) {
            setHistoryError('Failed to load leave history');
        } finally {
            setHistoryLoading(false);
        }
    };

    const handleCloseHistory = () => {
        setHistoryDialog({ open: false, member: null });
        setHistoryData([]);
        setHistoryError('');
    };

    return (
        <Box sx={{ p: 4 }}>
            <Typography variant="h4" sx={{ mb: 3 }}>Team Leave Balances</Typography>
            <Paper sx={{ p: 3, mb: 4 }}>
                {loadingTeam ? (
                    <CircularProgress />
                ) : teamError ? (
                    <Alert severity="error">{teamError}</Alert>
                ) : teamMembers.length === 0 ? (
                    <Alert severity="info">No team members found in your departments.</Alert>
                ) : (
                    <TableContainer>
                        <Table size="small">
                            <TableHead>
                                <TableRow>
                                    <TableCell>Staff Name</TableCell>
                                    <TableCell>Email</TableCell>
                                    <TableCell>Department</TableCell>
                                    <TableCell>Leave Type</TableCell>
                                    <TableCell>Remaining / Total Days</TableCell>
                                    <TableCell>Used Days</TableCell>
                                    <TableCell>Action</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {teamMembers.map(member => {
                                    const balances = teamBalances[member.id] || [];
                                    return balances.length === 0 ? (
                                        <TableRow key={member.id}>
                                            <TableCell>{member.firstName} {member.lastName}</TableCell>
                                            <TableCell>{member.email}</TableCell>
                                            <TableCell>{member.department}</TableCell>
                                            <TableCell colSpan={3}><i>No leave balances found</i></TableCell>
                                            <TableCell>
                                                <MuiButton size="small" variant="outlined" onClick={() => handleOpenHistory(member)}>
                                                    View History
                                                </MuiButton>
                                            </TableCell>
                                        </TableRow>
                                    ) : balances.map((bal, idx) => (
                                        <TableRow key={member.id + '-' + bal.leaveTypeName}>
                                            <TableCell>{member.firstName} {member.lastName}</TableCell>
                                            <TableCell>{member.email}</TableCell>
                                            <TableCell>{member.department}</TableCell>
                                            <TableCell>{bal.leaveTypeName}</TableCell>
                                            <TableCell>{bal.remainingDays} / {bal.totalDays}</TableCell>
                                            <TableCell>{bal.usedDays}</TableCell>
                                            {idx === 0 && (
                                                <TableCell rowSpan={balances.length}>
                                                    <MuiButton size="small" variant="outlined" onClick={() => handleOpenHistory(member)}>
                                                        View History
                                                    </MuiButton>
                                                </TableCell>
                                            )}
                                        </TableRow>
                                    ));
                                })}
                            </TableBody>
                        </Table>
                    </TableContainer>
                )}
            </Paper>
            <Dialog open={historyDialog.open} onClose={handleCloseHistory} maxWidth="md" fullWidth>
                <DialogTitle>Leave History for {historyDialog.member && `${historyDialog.member.firstName} ${historyDialog.member.lastName}`}</DialogTitle>
                <DialogContent>
                    {historyLoading ? (
                        <CircularProgress />
                    ) : historyError ? (
                        <Alert severity="error">{historyError}</Alert>
                    ) : historyData.length === 0 ? (
                        <Alert severity="info">No leave applications found</Alert>
                    ) : (
                        <Table size="small">
                            <TableHead>
                                <TableRow>
                                    <TableCell>Leave Type</TableCell>
                                    <TableCell>Start Date</TableCell>
                                    <TableCell>End Date</TableCell>
                                    <TableCell>Days</TableCell>
                                    <TableCell>Status</TableCell>
                                    <TableCell>Applied On</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {historyData.map(app => (
                                    <TableRow key={app.id}>
                                        <TableCell>{app.leaveType?.name || app.leaveTypeName || app.leaveType || '-'}</TableCell>
                                        <TableCell>{dayjs(app.startDate).format('MMM D, YYYY')}</TableCell>
                                        <TableCell>{dayjs(app.endDate).format('MMM D, YYYY')}</TableCell>
                                        <TableCell>{app.totalDays}</TableCell>
                                        <TableCell>{app.status}</TableCell>
                                        <TableCell>{dayjs(app.createdAt).format('MMM D, YYYY')}</TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    )}
                </DialogContent>
                <DialogActions>
                    <MuiButton onClick={handleCloseHistory}>Close</MuiButton>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default MyLeave; 