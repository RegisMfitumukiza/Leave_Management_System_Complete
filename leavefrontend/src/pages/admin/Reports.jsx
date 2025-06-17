import React, { useEffect, useState } from 'react';
import { Box, Typography, Paper, Button, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, MenuItem, FormControl, InputLabel, Select, TextField, CircularProgress, Snackbar, Alert } from '@mui/material';
import { authApi, leaveApi } from '@/lib/api';
import dayjs from 'dayjs';

const Reports = () => {
    const [reportType, setReportType] = useState('employee');
    const [users, setUsers] = useState([]);
    const [departments, setDepartments] = useState([]);
    const [leaveTypes, setLeaveTypes] = useState([]);
    const [selectedUser, setSelectedUser] = useState('');
    const [selectedDepartment, setSelectedDepartment] = useState('');
    const [selectedLeaveType, setSelectedLeaveType] = useState('');
    const [startDate, setStartDate] = useState(dayjs().startOf('year').format('YYYY-MM-DD'));
    const [endDate, setEndDate] = useState(dayjs().endOf('year').format('YYYY-MM-DD'));
    const [fileType, setFileType] = useState('excel');
    const [loading, setLoading] = useState(false);
    const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
    const [reports, setReports] = useState([]);
    const [fetchingReports, setFetchingReports] = useState(false);

    useEffect(() => {
        // Fetch users, departments, leave types
        authApi.get('/users').then(res => setUsers(res.data)).catch(() => setUsers([]));
        authApi.get('/departments').then(res => setDepartments(res.data)).catch(() => setDepartments([]));
        leaveApi.get('/leave-types').then(res => setLeaveTypes(res.data)).catch(() => setLeaveTypes([]));
        fetchReports();
    }, []);

    const fetchReports = async () => {
        setFetchingReports(true);
        try {
            const res = await leaveApi.get('/reports/date-range', {
                params: { startDate, endDate }
            });
            setReports(res.data);
        } catch {
            setReports([]);
        } finally {
            setFetchingReports(false);
        }
    };

    const handleGenerateReport = async (e) => {
        e.preventDefault();
        setLoading(true);
        setSnackbar({ open: false, message: '', severity: 'success' });
        try {
            let url = '';
            let params = { startDate, endDate, fileType };
            if (reportType === 'employee') {
                url = `/reports/employee/${selectedUser}`;
            } else if (reportType === 'department') {
                url = `/reports/department/${selectedDepartment}`;
            } else if (reportType === 'leaveType') {
                url = `/reports/leave-type/${selectedLeaveType}`;
            }
            await leaveApi.post(url, null, { params });
            setSnackbar({ open: true, message: 'Report generated successfully!', severity: 'success' });
            fetchReports();
        } catch (err) {
            setSnackbar({ open: true, message: 'Failed to generate report', severity: 'error' });
        } finally {
            setLoading(false);
        }
    };

    const handleDownload = async (reportId) => {
        try {
            const res = await leaveApi.get(`/reports/${reportId}/download`, {
                responseType: 'blob'
            });
            const url = window.URL.createObjectURL(new Blob([res.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `report_${reportId}.xlsx`); // or .csv
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch {
            setSnackbar({ open: true, message: 'Failed to download report', severity: 'error' });
        }
    };

    return (
        <Box sx={{ p: 4 }}>
            <Typography variant="h4" sx={{ mb: 3 }}>Reports</Typography>
            <Paper sx={{ p: 3, mb: 4 }}>
                <form onSubmit={handleGenerateReport} style={{ display: 'flex', gap: 16, flexWrap: 'wrap', alignItems: 'center' }}>
                    <FormControl sx={{ minWidth: 160 }}>
                        <InputLabel>Report Type</InputLabel>
                        <Select value={reportType} label="Report Type" onChange={e => setReportType(e.target.value)}>
                            <MenuItem value="employee">By Employee</MenuItem>
                            <MenuItem value="department">By Department</MenuItem>
                            <MenuItem value="leaveType">By Leave Type</MenuItem>
                        </Select>
                    </FormControl>
                    {reportType === 'employee' && (
                        <FormControl sx={{ minWidth: 160 }}>
                            <InputLabel>User</InputLabel>
                            <Select value={selectedUser} label="User" onChange={e => setSelectedUser(e.target.value)} required>
                                {users.map(u => <MenuItem key={u.id} value={u.id}>{u.firstName} {u.lastName}</MenuItem>)}
                            </Select>
                        </FormControl>
                    )}
                    {reportType === 'department' && (
                        <FormControl sx={{ minWidth: 160 }}>
                            <InputLabel>Department</InputLabel>
                            <Select value={selectedDepartment} label="Department" onChange={e => setSelectedDepartment(e.target.value)} required>
                                {departments.map(d => <MenuItem key={d.id} value={d.id}>{d.name}</MenuItem>)}
                            </Select>
                        </FormControl>
                    )}
                    {reportType === 'leaveType' && (
                        <FormControl sx={{ minWidth: 160 }}>
                            <InputLabel>Leave Type</InputLabel>
                            <Select value={selectedLeaveType} label="Leave Type" onChange={e => setSelectedLeaveType(e.target.value)} required>
                                {leaveTypes.map(lt => <MenuItem key={lt.id} value={lt.id}>{lt.name}</MenuItem>)}
                            </Select>
                        </FormControl>
                    )}
                    <TextField
                        label="Start Date"
                        type="date"
                        value={startDate}
                        onChange={e => setStartDate(e.target.value)}
                        InputLabelProps={{ shrink: true }}
                    />
                    <TextField
                        label="End Date"
                        type="date"
                        value={endDate}
                        onChange={e => setEndDate(e.target.value)}
                        InputLabelProps={{ shrink: true }}
                    />
                    <FormControl sx={{ minWidth: 120 }}>
                        <InputLabel>File Type</InputLabel>
                        <Select value={fileType} label="File Type" onChange={e => setFileType(e.target.value)}>
                            <MenuItem value="excel">Excel</MenuItem>
                            <MenuItem value="csv">CSV</MenuItem>
                        </Select>
                    </FormControl>
                    <Button type="submit" variant="contained" disabled={loading}>
                        {loading ? <CircularProgress size={20} /> : 'Generate Report'}
                    </Button>
                </form>
            </Paper>
            <Typography variant="h6" sx={{ mb: 2 }}>Generated Reports</Typography>
            {fetchingReports ? (
                <CircularProgress />
            ) : (
                <TableContainer component={Paper}>
                    <Table size="small">
                        <TableHead>
                            <TableRow>
                                <TableCell>Name</TableCell>
                                <TableCell>Type</TableCell>
                                <TableCell>Start Date</TableCell>
                                <TableCell>End Date</TableCell>
                                <TableCell>Generated By</TableCell>
                                <TableCell>Generated At</TableCell>
                                <TableCell>File Type</TableCell>
                                <TableCell>Download</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {reports.map(r => (
                                <TableRow key={r.id}>
                                    <TableCell>{r.name}</TableCell>
                                    <TableCell>{r.type}</TableCell>
                                    <TableCell>{r.startDate}</TableCell>
                                    <TableCell>{r.endDate}</TableCell>
                                    <TableCell>{r.generatedBy}</TableCell>
                                    <TableCell>{r.generatedAt}</TableCell>
                                    <TableCell>{r.fileType}</TableCell>
                                    <TableCell>
                                        <Button size="small" variant="outlined" onClick={() => handleDownload(r.id)}>Download</Button>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            )}
            <Snackbar open={snackbar.open} autoHideDuration={3000} onClose={() => setSnackbar({ ...snackbar, open: false })}>
                <Alert severity={snackbar.severity} sx={{ width: '100%' }}>{snackbar.message}</Alert>
            </Snackbar>
        </Box>
    );
};

export default Reports; 