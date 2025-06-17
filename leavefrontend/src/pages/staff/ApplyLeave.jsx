import React, { useState, useEffect } from 'react';
import {
    Box,
    Paper,
    Typography,
    TextField,
    Button,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    Grid,
    Alert,
    CircularProgress,
    FormHelperText,
    Chip,
    Stack,
    LinearProgress
} from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import dayjs from 'dayjs';
import { leaveApi } from '@/lib/api';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import DescriptionIcon from '@mui/icons-material/Description';
import EventIcon from '@mui/icons-material/Event';
import AttachFileIcon from '@mui/icons-material/AttachFile';

const API_BASE_URL = import.meta.env.VITE_API_URL;

const ApplyLeave = () => {
    const navigate = useNavigate();
    const { user } = useAuth();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState(false);
    const [leaveTypes, setLeaveTypes] = useState([]);
    const [selectedFiles, setSelectedFiles] = useState([]);
    const [formData, setFormData] = useState({
        leaveTypeId: '',
        startDate: null,
        endDate: null,
        reason: '',
        documents: []
    });
    const [formErrors, setFormErrors] = useState({
        leaveTypeId: '',
        startDate: '',
        endDate: '',
        reason: '',
        documents: ''
    });
    const [uploadProgress, setUploadProgress] = useState([]);
    const [uploadErrors, setUploadErrors] = useState([]);
    const [uploading, setUploading] = useState(false);

    useEffect(() => {
        const fetchLeaveTypes = async () => {
            try {
                const response = await leaveApi.get('/leave-types/active');
                setLeaveTypes(response.data);
            } catch (err) {
                setError('Failed to fetch leave types. Please try again.');
            }
        };

        fetchLeaveTypes();
    }, []);

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
        // Clear error when user starts typing
        if (formErrors[name]) {
            setFormErrors(prev => ({
                ...prev,
                [name]: ''
            }));
        }
    };

    const handleDateChange = (name, value) => {
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
        // Clear error when user selects a date
        if (formErrors[name]) {
            setFormErrors(prev => ({
                ...prev,
                [name]: ''
            }));
        }
    };

    const handleFileChange = (e) => {
        const files = Array.from(e.target.files);
        setSelectedFiles(prev => [...prev, ...files]);
    };

    const removeFile = (index) => {
        setSelectedFiles(prev => prev.filter((_, i) => i !== index));
    };

    // Helper: which leave types require a reason?
    const requiresReason = (() => {
        const selectedType = leaveTypes.find(type => type.id === formData.leaveTypeId);
        if (!selectedType) return false;
        // Adjust this list as needed
        const reasonRequiredTypes = ['Sick Leave', 'Maternity Leave', 'Compassionate Leave', 'Unpaid Leave'];
        return reasonRequiredTypes.includes(selectedType.name);
    })();

    const selectedType = leaveTypes.find(type => type.id === formData.leaveTypeId);
    const requiresDocument = selectedType?.requiresDocumentation || false;

    const validateForm = () => {
        const errors = {};
        const today = dayjs();

        if (!formData.leaveTypeId) {
            errors.leaveTypeId = 'Please select a leave type';
        }

        if (!formData.startDate) {
            errors.startDate = 'Please select a start date';
        } else if (formData.startDate.isBefore(today, 'day')) {
            errors.startDate = 'Start date cannot be in the past';
        }

        if (!formData.endDate) {
            errors.endDate = 'Please select an end date';
        } else if (formData.endDate.isBefore(formData.startDate, 'day')) {
            errors.endDate = 'End date cannot be before start date';
        }

        // Reason required logic
        if (requiresReason && !formData.reason.trim()) {
            errors.reason = 'Please provide a reason for leave';
        }

        // Document required logic
        if (requiresDocument && selectedFiles.length === 0) {
            errors.documents = 'Supporting document is required for this leave type';
        }

        setFormErrors(errors);
        return Object.keys(errors).length === 0;
    };

    const uploadDocuments = async () => {
        setUploading(true);
        setUploadProgress(Array(selectedFiles.length).fill(0));
        setUploadErrors(Array(selectedFiles.length).fill(null));
        const ids = [];
        for (let i = 0; i < selectedFiles.length; i++) {
            const file = selectedFiles[i];
            const formData = new FormData();
            formData.append('file', file);
            try {
                const res = await leaveApi.post('/documents/upload', formData, {
                    onUploadProgress: (event) => {
                        const percent = Math.round((event.loaded * 100) / event.total);
                        setUploadProgress(prev => {
                            const copy = [...prev];
                            copy[i] = percent;
                            return copy;
                        });
                    }
                });
                ids.push(res.data.id);
            } catch (err) {
                setUploadErrors(prev => {
                    const copy = [...prev];
                    copy[i] = err.response?.data?.message || 'Upload failed';
                    return copy;
                });
            }
        }
        setUploading(false);
        return ids;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!validateForm()) {
            return;
        }

        try {
            setLoading(true);
            setError('');
            setSuccess(false);
            let documentIds = [];
            if (selectedFiles.length > 0) {
                documentIds = await uploadDocuments();
                if (uploadErrors.some(e => e)) {
                    setError('Some documents failed to upload. Please fix and try again.');
                    setLoading(false);
                    return;
                }
            }
            const applicationData = {
                leaveTypeId: formData.leaveTypeId,
                startDate: formData.startDate.format('YYYY-MM-DD'),
                endDate: formData.endDate.format('YYYY-MM-DD'),
                reason: formData.reason,
                documentIds
            };
            await leaveApi.post('/leaves', applicationData);
            setSuccess(true);
            setTimeout(() => {
                navigate('/staff/dashboard');
            }, 2000);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to apply for leave');
        } finally {
            setLoading(false);
        }
    };

    return (
        <Box p={3}>
            <Paper sx={{ p: 3, maxWidth: 800, mx: 'auto' }}>
                <Typography variant="h5" sx={{ mb: 3 }}>Apply for Leave</Typography>

                {error && (
                    <Alert severity="error" sx={{ mb: 3 }}>
                        {error}
                    </Alert>
                )}

                {success && (
                    <Alert severity="success" sx={{ mb: 3 }}>
                        Leave application submitted successfully! Redirecting to dashboard...
                    </Alert>
                )}

                <form onSubmit={handleSubmit}>
                    <Grid container spacing={3}>
                        {/* Leave Type */}
                        <Grid item xs={12}>
                            <FormControl fullWidth error={!!formErrors.leaveTypeId}>
                                <InputLabel>Leave Type</InputLabel>
                                <Select
                                    name="leaveTypeId"
                                    value={formData.leaveTypeId}
                                    onChange={handleInputChange}
                                    label="Leave Type"
                                >
                                    {leaveTypes.map(type => (
                                        <MenuItem key={type.id} value={type.id}>
                                            {type.name} ({type.defaultDays} days)
                                        </MenuItem>
                                    ))}
                                </Select>
                                {formErrors.leaveTypeId && (
                                    <FormHelperText>{formErrors.leaveTypeId}</FormHelperText>
                                )}
                            </FormControl>
                        </Grid>

                        {/* Date Range */}
                        <Grid item xs={12} sm={6}>
                            <LocalizationProvider dateAdapter={AdapterDayjs}>
                                <DatePicker
                                    label="Start Date"
                                    value={formData.startDate}
                                    onChange={(value) => handleDateChange('startDate', value)}
                                    slotProps={{
                                        textField: {
                                            fullWidth: true,
                                            error: !!formErrors.startDate,
                                            helperText: formErrors.startDate
                                        }
                                    }}
                                />
                            </LocalizationProvider>
                        </Grid>

                        <Grid item xs={12} sm={6}>
                            <LocalizationProvider dateAdapter={AdapterDayjs}>
                                <DatePicker
                                    label="End Date"
                                    value={formData.endDate}
                                    onChange={(value) => handleDateChange('endDate', value)}
                                    slotProps={{
                                        textField: {
                                            fullWidth: true,
                                            error: !!formErrors.endDate,
                                            helperText: formErrors.endDate
                                        }
                                    }}
                                />
                            </LocalizationProvider>
                        </Grid>

                        {/* Reason */}
                        <Grid item xs={12}>
                            <TextField
                                fullWidth
                                multiline
                                rows={4}
                                label={`Reason for Leave ${requiresReason ? '(Required)' : '(Optional)'}`}
                                name="reason"
                                value={formData.reason}
                                onChange={handleInputChange}
                                error={!!formErrors.reason}
                                helperText={formErrors.reason}
                            />
                        </Grid>

                        {/* Document Upload */}
                        <Grid item xs={12}>
                            <Box sx={{ mb: 2 }}>
                                <Typography variant="subtitle1" sx={{ mb: 1 }}>
                                    Supporting Documents {requiresDocument ? <span style={{ color: 'red' }}>(Required)</span> : '(Optional)'}
                                </Typography>
                                {selectedType && (requiresDocument || selectedFiles.length > 0) && (
                                    <Box>
                                        <Button component="label" startIcon={<AttachFileIcon />} variant="outlined">
                                            {requiresDocument ? 'Upload Document (Required)' : 'Upload Document (Optional)'}
                                            <input
                                                type="file"
                                                hidden
                                                multiple
                                                onChange={handleFileChange}
                                                required={requiresDocument}
                                            />
                                        </Button>
                                        {formErrors.documents && <FormHelperText error>{formErrors.documents}</FormHelperText>}
                                        <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
                                            {selectedFiles.map((file, idx) => (
                                                <Chip
                                                    key={idx}
                                                    icon={<DescriptionIcon />}
                                                    label={file.name}
                                                    onDelete={() => removeFile(idx)}
                                                />
                                            ))}
                                        </Stack>
                                    </Box>
                                )}
                            </Box>
                        </Grid>

                        {/* Submit Button */}
                        <Grid item xs={12}>
                            <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
                                <Button
                                    variant="outlined"
                                    onClick={() => navigate('/staff/dashboard')}
                                    disabled={loading}
                                >
                                    Cancel
                                </Button>
                                <Button
                                    type="submit"
                                    variant="contained"
                                    color="primary"
                                    disabled={loading || uploading}
                                    startIcon={loading ? <CircularProgress size={20} /> : <DescriptionIcon />}
                                >
                                    {loading ? 'Submitting...' : 'Submit Application'}
                                </Button>
                            </Box>
                        </Grid>
                    </Grid>
                </form>
            </Paper>
        </Box>
    );
};

export default ApplyLeave; 