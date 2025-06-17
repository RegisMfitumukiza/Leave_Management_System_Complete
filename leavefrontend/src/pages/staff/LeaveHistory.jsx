import React, { useEffect, useState } from 'react';
import {
    Box, Paper, Typography, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Button, Chip, Grid, Select, MenuItem, InputLabel, FormControl, TextField, Dialog, DialogTitle, DialogContent, DialogActions, CircularProgress, Alert, Stack
} from '@mui/material';
import { DatePicker, LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import dayjs from 'dayjs';
import { leaveApi } from '@/lib/api';
import DescriptionIcon from '@mui/icons-material/Description';
import DownloadIcon from '@mui/icons-material/Download';

const API_BASE_URL = import.meta.env.VITE_API_URL;

const statusColors = {
    APPROVED: 'success',
    REJECTED: 'error',
    PENDING: 'warning',
};

const LeaveHistory = () => {
    const [applications, setApplications] = useState([]);
    const [filtered, setFiltered] = useState([]);
    const [leaveTypes, setLeaveTypes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [filters, setFilters] = useState({
        leaveType: '',
        status: '',
        start: null,
        end: null,
        search: '',
    });
    const [selected, setSelected] = useState(null);
    const [modalOpen, setModalOpen] = useState(false);

    useEffect(() => {
        setLoading(true);
        Promise.all([
            leaveApi.get('/leaves/me'),
            leaveApi.get('/leave-types/active'),
        ])
            .then(([appsRes, typesRes]) => {
                setApplications(appsRes.data);
                setLeaveTypes(typesRes.data);
                setFiltered(appsRes.data);
            })
            .catch(() => setError('Failed to load leave history'))
            .finally(() => setLoading(false));
    }, []);

    useEffect(() => {
        let data = [...applications];
        if (filters.leaveType) data = data.filter(a => a.leaveTypeId === filters.leaveType);
        if (filters.status) data = data.filter(a => a.status === filters.status);
        if (filters.start) data = data.filter(a => dayjs(a.startDate).isAfter(dayjs(filters.start).subtract(1, 'day')));
        if (filters.end) data = data.filter(a => dayjs(a.endDate).isBefore(dayjs(filters.end).add(1, 'day')));
        if (filters.search) {
            const s = filters.search.toLowerCase();
            data = data.filter(a =>
                a.reason?.toLowerCase().includes(s) ||
                a.leaveTypeName?.toLowerCase().includes(s)
            );
        }
        setFiltered(data);
    }, [filters, applications]);

    const handleFilterChange = (e) => {
        const { name, value } = e.target;
        setFilters(f => ({ ...f, [name]: value }));
    };

    const handleDateChange = (name, value) => {
        setFilters(f => ({ ...f, [name]: value }));
    };

    const openModal = (app) => {
        setSelected(app);
        setModalOpen(true);
    };
    const closeModal = () => {
        setModalOpen(false);
        setSelected(null);
    };

    const handleCancel = async (leave) => {
        try {
            await leaveApi.post(`/leaves/${leave.id}/cancel`);
            const updatedApplications = applications.filter(a => a.id !== leave.id);
            setApplications(updatedApplications);
            setFiltered(updatedApplications);
            closeModal();
        } catch (err) {
            setError('Failed to cancel the leave.');
        }
    };

    const handleViewDownload = async (doc) => {
        try {
            const res = await leaveApi.get(`/documents/download/${doc.fileName}`, { responseType: 'blob' });
            const type = doc.fileType || 'application/octet-stream';
            const url = window.URL.createObjectURL(new Blob([res.data], { type }));
            const newWindow = window.open(url, '_blank');
            if (!newWindow) {
                const link = document.createElement('a');
                link.href = url;
                link.setAttribute('download', doc.fileName);
                document.body.appendChild(link);
                link.click();
                link.remove();
            }
            setTimeout(() => window.URL.revokeObjectURL(url), 1000);
        } catch (err) {
            alert('Failed to download document');
        }
    };

    return (
        <Box p={3}>
            <Paper sx={{ p: 3, mb: 3 }}>
                <Typography variant="h5" sx={{ mb: 2 }}>Leave History</Typography>
                {/* Filters */}
                <Grid container spacing={2} alignItems="center" sx={{ mb: 2 }}>
                    <Grid item xs={12} sm={3}>
                        <FormControl fullWidth>
                            <InputLabel>Leave Type</InputLabel>
                            <Select name="leaveType" value={filters.leaveType} label="Leave Type" onChange={handleFilterChange}>
                                <MenuItem value="">All</MenuItem>
                                {leaveTypes.map(type => (
                                    <MenuItem key={type.id} value={type.id}>{type.name}</MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                    </Grid>
                    <Grid item xs={12} sm={2}>
                        <FormControl fullWidth>
                            <InputLabel>Status</InputLabel>
                            <Select name="status" value={filters.status} label="Status" onChange={handleFilterChange}>
                                <MenuItem value="">All</MenuItem>
                                <MenuItem value="APPROVED">Approved</MenuItem>
                                <MenuItem value="REJECTED">Rejected</MenuItem>
                                <MenuItem value="PENDING">Pending</MenuItem>
                            </Select>
                        </FormControl>
                    </Grid>
                    <Grid item xs={12} sm={2}>
                        <LocalizationProvider dateAdapter={AdapterDayjs}>
                            <DatePicker label="Start Date" value={filters.start} onChange={v => handleDateChange('start', v)}
                                slotProps={{ textField: { fullWidth: true } }} />
                        </LocalizationProvider>
                    </Grid>
                    <Grid item xs={12} sm={2}>
                        <LocalizationProvider dateAdapter={AdapterDayjs}>
                            <DatePicker label="End Date" value={filters.end} onChange={v => handleDateChange('end', v)}
                                slotProps={{ textField: { fullWidth: true } }} />
                        </LocalizationProvider>
                    </Grid>
                    <Grid item xs={12} sm={3}>
                        <TextField fullWidth label="Search Reason/Type" name="search" value={filters.search} onChange={handleFilterChange} />
                    </Grid>
                </Grid>
                {/* Table */}
                {loading ? (
                    <Box display="flex" justifyContent="center" alignItems="center" minHeight="200px"><CircularProgress /></Box>
                ) : error ? (
                    <Alert severity="error">{error}</Alert>
                ) : filtered.length === 0 ? (
                    <Alert severity="info">No leave applications found.</Alert>
                ) : (
                    <TableContainer>
                        <Table size="small">
                            <TableHead>
                                <TableRow>
                                    <TableCell>Type</TableCell>
                                    <TableCell>Dates</TableCell>
                                    <TableCell>Status</TableCell>
                                    <TableCell>Reason</TableCell>
                                    <TableCell>Documents</TableCell>
                                    <TableCell>Actions</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {filtered.map(app => (
                                    <TableRow key={app.id}>
                                        <TableCell>{app.leaveTypeName}</TableCell>
                                        <TableCell>{dayjs(app.startDate).format('MMM D, YYYY')} - {dayjs(app.endDate).format('MMM D, YYYY')}</TableCell>
                                        <TableCell><Chip label={app.status} color={statusColors[app.status] || 'default'} size="small" /></TableCell>
                                        <TableCell>{app.reason}</TableCell>
                                        <TableCell>
                                            {app.documents && app.documents.length > 0 ? (
                                                <Stack direction="row" spacing={1}>
                                                    {app.documents.map(doc => (
                                                        <Button key={doc.id} size="small" onClick={() => handleViewDownload(doc)} startIcon={<DownloadIcon />}>{doc.fileName}</Button>
                                                    ))}
                                                </Stack>
                                            ) : '—'}
                                        </TableCell>
                                        <TableCell>
                                            <Button size="small" variant="outlined" startIcon={<DescriptionIcon />} onClick={() => openModal(app)}>
                                                View Details
                                            </Button>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                )}
            </Paper>
            {/* Details Modal */}
            <Dialog open={modalOpen} onClose={closeModal} maxWidth="sm" fullWidth>
                <DialogTitle>Leave Application Details</DialogTitle>
                <DialogContent dividers>
                    {selected && (
                        <Box>
                            <Typography variant="subtitle1"><b>Type:</b> {selected.leaveTypeName}</Typography>
                            <Typography variant="subtitle1"><b>Status:</b> <Chip label={selected.status} color={statusColors[selected.status] || 'default'} size="small" /></Typography>
                            <Typography variant="subtitle1"><b>Dates:</b> {dayjs(selected.startDate).format('MMM D, YYYY')} - {dayjs(selected.endDate).format('MMM D, YYYY')}</Typography>
                            <Typography variant="subtitle1"><b>Reason:</b> {selected.reason}</Typography>
                            <Typography variant="subtitle1"><b>Comments:</b> {selected.comments || '—'}</Typography>
                            <Typography variant="subtitle1"><b>Documents:</b></Typography>
                            {selected && selected.documents && selected.documents.length > 0 ? (
                                <Stack direction="row" spacing={1}>
                                    {selected.documents.map(doc => (
                                        <Button key={doc.id} size="small" onClick={() => handleViewDownload(doc)} startIcon={<DownloadIcon />}>{doc.fileName}</Button>
                                    ))}
                                </Stack>
                            ) : '—'}
                        </Box>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={closeModal}>Close</Button>
                    <Button onClick={() => handleCancel(selected)}>Cancel</Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default LeaveHistory; 