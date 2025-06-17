import React, { useState, useEffect } from 'react';
import {
    Box,
    Typography,
    Paper,
    MenuItem,
    Select,
    FormControl,
    InputLabel,
    Button,
    TextField,
    Grid,
    Alert,
    CircularProgress,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Snackbar
} from '@mui/material';
import dayjs from 'dayjs';
import { leaveApi } from '@/lib/api';
import DownloadIcon from '@mui/icons-material/Download';

const Reports = () => {
    const [reportType, setReportType] = useState('team-leave');
    const [startDate, setStartDate] = useState(dayjs().startOf('month').format('YYYY-MM-DD'));
    const [endDate, setEndDate] = useState(dayjs().endOf('month').format('YYYY-MM-DD'));
    const [fileType, setFileType] = useState('csv');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [reports, setReports] = useState([]);
    const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

    // Fetch existing reports
    useEffect(() => {
        fetchReports();
    }, []);

    const fetchReports = async () => {
        try {
            const res = await leaveApi.get('/reports/date-range', {
                params: { startDate, endDate }
            });
            setReports(res.data);
        } catch (err) {
            setError('Failed to fetch reports');
        }
    };

    const handleGenerateReport = async () => {
        setLoading(true);
        setError('');
        try {
            const params = {
                startDate,
                endDate,
                fileType
            };

            let url = '';
            switch (reportType) {
                case 'team-leave':
                    url = '/reports/manager/team-leave';
                    break;
                case 'approval':
                    url = '/reports/manager/approval-stats';
                    break;
                case 'coverage':
                    url = '/reports/manager/team-coverage';
                    break;
                default:
                    throw new Error('Invalid report type');
            }

            const res = await leaveApi.post(url, null, { params });
            setReports([{
                ...res.data,
                fileUrl: `${import.meta.env.VITE_LEAVE_SERVICE_URL}/reports/${res.data.id}/download`
            }, ...reports]);

            setSnackbar({
                open: true,
                message: 'Report generated successfully',
                severity: 'success'
            });
        } catch (err) {
            let errorMsg = err?.response?.data;
            if (typeof errorMsg === 'object') {
                errorMsg = errorMsg.error || errorMsg.message || JSON.stringify(errorMsg);
            }
            setError(errorMsg || err.message || 'Failed to generate report');
            setSnackbar({
                open: true,
                message: 'Failed to generate report',
                severity: 'error'
            });
        } finally {
            setLoading(false);
        }
    };

    const handleDownload = async (reportId, fileType) => {
        try {
            const res = await leaveApi.get(`/reports/${reportId}/download`, { responseType: 'blob' });
            const contentType = res.headers['content-type'] || (fileType === 'csv' ? 'text/csv' : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
            const url = window.URL.createObjectURL(new Blob([res.data], { type: contentType }));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `report_${reportId}.${fileType === 'csv' ? 'csv' : 'xlsx'}`);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
        } catch (err) {
            setSnackbar({ open: true, message: 'Failed to download report', severity: 'error' });
        }
    };

    return (
        <Box sx={{ p: 4 }}>
            <Paper sx={{ p: 3, mb: 3 }}>
                <Typography variant="h4" sx={{ mb: 2 }}>Team Reports</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                    Generate reports for your team's leave status, approvals, and department coverage.
                </Typography>
                <Grid container spacing={2} alignItems="center">
                    <Grid item xs={12} md={3}>
                        <FormControl fullWidth>
                            <InputLabel>Report Type</InputLabel>
                            <Select
                                value={reportType}
                                label="Report Type"
                                onChange={e => setReportType(e.target.value)}
                            >
                                <MenuItem value="team-leave">
                                    <Box>
                                        <Typography>Team Leave Status</Typography>
                                        <Typography variant="caption" color="text.secondary">
                                            View team member leave status and calendar
                                        </Typography>
                                    </Box>
                                </MenuItem>
                                <MenuItem value="approval">
                                    <Box>
                                        <Typography>Approval Summary</Typography>
                                        <Typography variant="caption" color="text.secondary">
                                            Track leave approvals and response times
                                        </Typography>
                                    </Box>
                                </MenuItem>
                                <MenuItem value="coverage">
                                    <Box>
                                        <Typography>Department Coverage</Typography>
                                        <Typography variant="caption" color="text.secondary">
                                            Monitor team availability and coverage
                                        </Typography>
                                    </Box>
                                </MenuItem>
                            </Select>
                        </FormControl>
                    </Grid>
                    <Grid item xs={12} md={3}>
                        <TextField
                            label="Start Date"
                            type="date"
                            value={startDate}
                            onChange={e => setStartDate(e.target.value)}
                            fullWidth
                            InputLabelProps={{ shrink: true }}
                            helperText="Select report period start"
                        />
                    </Grid>
                    <Grid item xs={12} md={3}>
                        <TextField
                            label="End Date"
                            type="date"
                            value={endDate}
                            onChange={e => setEndDate(e.target.value)}
                            fullWidth
                            InputLabelProps={{ shrink: true }}
                            helperText="Select report period end"
                        />
                    </Grid>
                    <Grid item xs={12} md={3}>
                        <FormControl fullWidth>
                            <InputLabel>Export Format</InputLabel>
                            <Select
                                value={fileType}
                                label="Export Format"
                                onChange={e => setFileType(e.target.value)}
                            >
                                <MenuItem value="excel">Excel (.xlsx)</MenuItem>
                                <MenuItem value="csv">CSV (.csv)</MenuItem>
                            </Select>
                        </FormControl>
                    </Grid>
                    <Grid item xs={12}>
                        <Button
                            variant="contained"
                            onClick={handleGenerateReport}
                            disabled={loading}
                            startIcon={loading ? <CircularProgress size={20} /> : null}
                        >
                            {loading ? 'Generating...' : 'Generate Report'}
                        </Button>
                    </Grid>
                </Grid>
                {error && <Alert severity="error" sx={{ mt: 2 }}>{String(error)}</Alert>}
            </Paper>

            <Paper sx={{ p: 3 }}>
                <Typography variant="h6" sx={{ mb: 2 }}>Generated Reports</Typography>
                {reports.length === 0 ? (
                    <Alert severity="info">
                        No reports generated yet. Use the form above to generate a report.
                    </Alert>
                ) : (
                    <TableContainer>
                        <Table>
                            <TableHead>
                                <TableRow>
                                    <TableCell>Report Name</TableCell>
                                    <TableCell>Type</TableCell>
                                    <TableCell>Period</TableCell>
                                    <TableCell>Generated</TableCell>
                                    <TableCell>Format</TableCell>
                                    <TableCell>Action</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {reports.map(rpt => (
                                    <TableRow key={rpt.id}>
                                        <TableCell>{rpt.name}</TableCell>
                                        <TableCell>
                                            {rpt.type === 'team-leave' && 'Team Leave Status'}
                                            {rpt.type === 'approval' && 'Approval Summary'}
                                            {rpt.type === 'coverage' && 'Department Coverage'}
                                        </TableCell>
                                        <TableCell>
                                            {dayjs(rpt.startDate).format('MMM D, YYYY')} - {dayjs(rpt.endDate).format('MMM D, YYYY')}
                                        </TableCell>
                                        <TableCell>{dayjs(rpt.generatedAt).format('MMM D, YYYY HH:mm')}</TableCell>
                                        <TableCell>{rpt.fileType.toUpperCase()}</TableCell>
                                        <TableCell>
                                            <Button
                                                variant="outlined"
                                                size="small"
                                                onClick={() => handleDownload(rpt.id, rpt.fileType)}
                                                startIcon={<DownloadIcon />}
                                            >
                                                Download
                                            </Button>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                )}
            </Paper>

            <Snackbar
                open={snackbar.open}
                autoHideDuration={3000}
                onClose={() => setSnackbar({ ...snackbar, open: false })}
            >
                <Alert severity={snackbar.severity} sx={{ width: '100%' }}>
                    {snackbar.message}
                </Alert>
            </Snackbar>
        </Box>
    );
};

export default Reports; 