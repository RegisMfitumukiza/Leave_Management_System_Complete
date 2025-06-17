import React, { useEffect, useState } from 'react';
import { Box, Typography, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, CircularProgress, Alert, Button, Dialog, DialogTitle, DialogContent, DialogActions, TextField, Snackbar, Chip } from '@mui/material';
import { useAuth } from '../../contexts/AuthContext';
import { leaveApi } from '@/lib/api';
import { authApi } from '@/lib/api';
import dayjs from 'dayjs';

const Documents = () => {
    const { user } = useAuth();
    const [documents, setDocuments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [actionDialog, setActionDialog] = useState({ open: false, doc: null, action: '' });
    const [actionComment, setActionComment] = useState('');
    const [actionLoading, setActionLoading] = useState(false);
    const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

    useEffect(() => {
        const fetchDocuments = async () => {
            setLoading(true);
            setError('');
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
                const memberIds = uniqueTeamMembers.map(u => u.id);
                // 2. Fetch documents for all team members
                let allDocs = [];
                for (const memberId of memberIds) {
                    const res = await leaveApi.get(`/documents/my-documents`, {
                        params: { userId: memberId }
                    });
                    allDocs = allDocs.concat(res.data);
                }
                setDocuments(allDocs);
            } catch (err) {
                setError('Failed to load documents.');
            } finally {
                setLoading(false);
            }
        };
        if (user) fetchDocuments();
    }, [user]);

    const handleAction = (doc, action) => {
        setActionDialog({ open: true, doc, action });
        setActionComment('');
    };
    const handleActionClose = () => {
        setActionDialog({ open: false, doc: null, action: '' });
        setActionComment('');
    };
    // Helper to display user-friendly status
    const getStatusLabel = (status) => {
        if (!status) return 'Pending';
        if (status.toUpperCase() === 'APPROVED') return 'Approved';
        if (status.toUpperCase() === 'REJECTED') return 'Rejected';
        return status.charAt(0) + status.slice(1).toLowerCase();
    };

    const handleActionSubmit = async () => {
        setActionLoading(true);
        try {
            const url = `${import.meta.env.VITE_API_URL}/documents/${actionDialog.doc.id}/${actionDialog.action}`;
            const res = await leaveApi.post(url, actionComment ? { comment: actionComment } : {});
            setSnackbar({ open: true, message: `Document ${actionDialog.action}d successfully!`, severity: 'success' });
            // Use the updated document from backend
            setDocuments(docs => docs.map(d => d.id === actionDialog.doc.id ? { ...d, ...res.data } : d));
            handleActionClose();
        } catch (err) {
            setSnackbar({ open: true, message: 'Failed to process action.', severity: 'error' });
        } finally {
            setActionLoading(false);
        }
    };

    const handleViewDownload = async (doc) => {
        try {
            const res = await leaveApi.get(`/documents/download/${doc.fileName}`, { responseType: 'blob' });
            // Use the fileType from the doc if available, fallback to application/octet-stream
            const type = doc.fileType || 'application/octet-stream';
            const url = window.URL.createObjectURL(new Blob([res.data], { type }));
            // Try to open in new tab for view, fallback to download
            const newWindow = window.open(url, '_blank');
            if (!newWindow) {
                // If popup blocked, fallback to download
                const link = document.createElement('a');
                link.href = url;
                link.setAttribute('download', doc.fileName);
                document.body.appendChild(link);
                link.click();
                link.remove();
            }
            setTimeout(() => window.URL.revokeObjectURL(url), 1000);
        } catch (err) {
            setSnackbar({ open: true, message: 'Failed to download document', severity: 'error' });
        }
    };

    return (
        <Box sx={{ p: 4 }}>
            <Paper sx={{ p: 3 }}>
                <Typography variant="h4" sx={{ mb: 2 }}>Team Documents</Typography>
                {loading ? (
                    <Box display="flex" justifyContent="center" alignItems="center" minHeight="200px">
                        <CircularProgress />
                    </Box>
                ) : error ? (
                    <Alert severity="error">{error}</Alert>
                ) : documents.length === 0 ? (
                    <Alert severity="info">No documents found for your team.</Alert>
                ) : (
                    <TableContainer>
                        <Table size="small">
                            <TableHead>
                                <TableRow>
                                    <TableCell>Employee</TableCell>
                                    <TableCell>Leave Type</TableCell>
                                    <TableCell>Document Name</TableCell>
                                    <TableCell>Uploaded At</TableCell>
                                    <TableCell>Status</TableCell>
                                    <TableCell>Actions</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {documents.map((doc, index) => (
                                    <TableRow key={doc.id + '-' + doc.uploadedAt + '-' + index}>
                                        <TableCell>{doc.employeeName || '-'}</TableCell>
                                        <TableCell>{doc.leaveTypeName || '-'}</TableCell>
                                        <TableCell>{doc.fileName}</TableCell>
                                        <TableCell>{dayjs(doc.uploadedAt).format('YYYY-MM-DD HH:mm')}</TableCell>
                                        <TableCell>
                                            <Chip label={getStatusLabel(doc.status)} color={
                                                doc.status && doc.status.toUpperCase() === 'APPROVED' ? 'success' :
                                                    doc.status && doc.status.toUpperCase() === 'REJECTED' ? 'error' :
                                                        'warning'
                                            } size="small" />
                                        </TableCell>
                                        <TableCell>
                                            <Button
                                                size="small"
                                                variant="outlined"
                                                onClick={() => handleViewDownload(doc)}
                                                sx={{ mr: 1 }}
                                            >
                                                View/Download
                                            </Button>
                                            {(doc.status === undefined || doc.status === null || doc.status === 'Pending') && (
                                                <>
                                                    <Button
                                                        size="small"
                                                        variant="contained"
                                                        color="success"
                                                        sx={{ mr: 1 }}
                                                        onClick={() => handleAction(doc, 'approve')}
                                                    >
                                                        Approve
                                                    </Button>
                                                    <Button
                                                        size="small"
                                                        variant="contained"
                                                        color="error"
                                                        onClick={() => handleAction(doc, 'reject')}
                                                    >
                                                        Reject
                                                    </Button>
                                                </>
                                            )}
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                )}
            </Paper>
            <Dialog open={actionDialog.open} onClose={handleActionClose}>
                <DialogTitle>{actionDialog.action === 'approve' ? 'Approve Document' : 'Reject Document'}</DialogTitle>
                <DialogContent>
                    <TextField
                        margin="dense"
                        label="Comment (optional)"
                        fullWidth
                        multiline
                        rows={2}
                        value={actionComment}
                        onChange={(e) => setActionComment(e.target.value)}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleActionClose} disabled={actionLoading}>Cancel</Button>
                    <Button onClick={handleActionSubmit} disabled={actionLoading} color={actionDialog.action === 'approve' ? 'success' : 'error'}>
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

export default Documents; 