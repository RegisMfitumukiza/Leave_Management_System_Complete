import React, { useEffect, useState } from 'react';
import { Box, Typography, Paper, CircularProgress, Alert, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Button, Dialog, DialogTitle, DialogContent, DialogActions, TextField, Snackbar } from '@mui/material';
import { leaveApi } from '@/lib/api';
import { useAuth } from '../../contexts/AuthContext';
import dayjs from 'dayjs';

const LeaveApprovals = () => {
    const { user } = useAuth();
    const [pendingApprovals, setPendingApprovals] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [actionDialog, setActionDialog] = useState({ open: false, app: null, action: '' });
    const [actionComment, setActionComment] = useState('');
    const [actionLoading, setActionLoading] = useState(false);
    const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

    useEffect(() => {
        fetchPendingApprovals();
    }, []);

    const fetchPendingApprovals = async () => {
        setLoading(true);
        setError('');
        try {
            const res = await leaveApi.get('/leaves/pending');
            setPendingApprovals(res.data);
        } catch (err) {
            setError('Failed to load pending approvals');
            console.error('Pending approvals fetch error:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleAction = (app, action) => {
        setActionDialog({ open: true, app, action });
        setActionComment('');
    };

    const handleActionClose = () => {
        setActionDialog({ open: false, app: null, action: '' });
        setActionComment('');
    };

    const handleApprove = async () => {
        if (!actionDialog.app) return;
        setActionLoading(true);
        try {
            await leaveApi.post(`/leaves/${actionDialog.app.id}/approve`, {
                comments: actionComment
            });
            handleActionClose();
            fetchPendingApprovals();
            setSnackbar({ open: true, message: 'Leave application approved successfully', severity: 'success' });
        } catch (err) {
            const errorMsg = err.response?.data?.message || err.response?.data?.error || (typeof err.response?.data === 'string' ? err.response.data : null) || 'Failed to approve leave application';
            setSnackbar({ open: true, message: errorMsg, severity: 'error' });
        } finally {
            setActionLoading(false);
        }
    };

    const handleReject = async () => {
        if (!actionDialog.app) return;
        setActionLoading(true);
        try {
            await leaveApi.post(`/leaves/${actionDialog.app.id}/reject`, {
                comments: actionComment
            });
            handleActionClose();
            fetchPendingApprovals();
            setSnackbar({ open: true, message: 'Leave application rejected successfully', severity: 'success' });
        } catch (err) {
            const errorMsg = err.response?.data?.message || err.response?.data?.error || (typeof err.response?.data === 'string' ? err.response.data : null) || 'Failed to reject leave application';
            setSnackbar({ open: true, message: errorMsg, severity: 'error' });
        } finally {
            setActionLoading(false);
        }
    };

    return (
        <Box sx={{ p: 4 }}>
            <Typography variant="h4" sx={{ mb: 3 }}>Leave Approvals</Typography>
            <Paper sx={{ p: 3 }}>
                {loading ? (
                    <CircularProgress />
                ) : error ? (
                    <Alert severity="error">{error}</Alert>
                ) : pendingApprovals.length === 0 ? (
                    <Alert severity="info">No pending leave approvals.</Alert>
                ) : (
                    <TableContainer>
                        <Table size="small">
                            <TableHead>
                                <TableRow>
                                    <TableCell>Employee</TableCell>
                                    <TableCell>Leave Type</TableCell>
                                    <TableCell>Start Date</TableCell>
                                    <TableCell>End Date</TableCell>
                                    <TableCell>Reason</TableCell>
                                    <TableCell>Applied On</TableCell>
                                    <TableCell>Actions</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {pendingApprovals.map((app) => (
                                    <TableRow key={app.id}>
                                        <TableCell>{app.user?.firstName || app.employeeName || '-'} {app.user?.lastName || ''}</TableCell>
                                        <TableCell>{app.leaveType?.name || app.leaveTypeName || '-'}</TableCell>
                                        <TableCell>{dayjs(app.startDate).format('MMM D, YYYY')}</TableCell>
                                        <TableCell>{dayjs(app.endDate).format('MMM D, YYYY')}</TableCell>
                                        <TableCell>{app.reason || '-'}</TableCell>
                                        <TableCell>{dayjs(app.createdAt).format('MMM D, YYYY')}</TableCell>
                                        <TableCell>
                                            <Button variant="outlined" color="primary" onClick={() => handleAction(app, 'approve')} sx={{ mr: 1 }}>Approve</Button>
                                            <Button variant="outlined" color="error" onClick={() => handleAction(app, 'reject')}>Reject</Button>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                )}
            </Paper>
            <Dialog open={actionDialog.open} onClose={handleActionClose}>
                <DialogTitle> {actionDialog.action === 'approve' ? 'Approve Leave' : 'Reject Leave'}</DialogTitle>
                <DialogContent>
                    <TextField
                        margin="dense"
                        label="Comments (optional)"
                        fullWidth
                        multiline
                        rows={2}
                        value={actionComment}
                        onChange={(e) => setActionComment(e.target.value)}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleActionClose} disabled={actionLoading}>Cancel</Button>
                    <Button onClick={actionDialog.action === 'approve' ? handleApprove : handleReject} disabled={actionLoading} color={actionDialog.action === 'approve' ? 'primary' : 'error'}>
                        {actionLoading ? <CircularProgress size={20} /> : (actionDialog.action === 'approve' ? 'Approve' : 'Reject')}
                    </Button>
                </DialogActions>
            </Dialog>
            <Snackbar open={snackbar.open} autoHideDuration={3000} onClose={() => setSnackbar({ ...snackbar, open: false })}>
                <Alert severity={snackbar.severity} onClose={() => setSnackbar({ ...snackbar, open: false })}>
                    {snackbar.message}
                </Alert>
            </Snackbar>
        </Box>
    );
};

export default LeaveApprovals; 