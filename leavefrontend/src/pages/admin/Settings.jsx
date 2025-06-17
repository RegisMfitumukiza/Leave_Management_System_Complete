import React, { useEffect, useState } from 'react';
import { Box, Typography, Paper, TextField, Button, Switch, FormControlLabel, Snackbar, Alert, Grid, MenuItem, Select, InputLabel, FormControl, Checkbox, ListItemText, OutlinedInput } from '@mui/material';
import { leaveApi } from '@/lib/api';

const Settings = () => {
    const [settings, setSettings] = useState({
        accrualRate: 1.66,
        maxCarryover: 5,
        carryoverExpiryDate: '2024-01-31',
        notificationPreferences: 'email,in-app',
        documentRequiredFor: '',
    });
    const [leaveTypes, setLeaveTypes] = useState([]);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

    // Fetch leave types for multi-select
    useEffect(() => {
        leaveApi.get('/leave-types')
            .then(res => setLeaveTypes(res.data.map(lt => lt.name)))
            .catch(() => setLeaveTypes([]));
    }, []);

    useEffect(() => {
        setLoading(true);
        leaveApi.get('/settings')
            .then(res => {
                const np = res.data.notificationPreferences || '';
                setSettings({
                    ...res.data,
                    emailNotifications: np.includes('email'),
                    inAppNotifications: np.includes('in-app'),
                    documentRequiredFor: res.data.documentRequiredFor
                        ? res.data.documentRequiredFor.split(',').filter(Boolean)
                        : [],
                });
            })
            .catch(() => setSnackbar({ open: true, message: 'Failed to load settings', severity: 'error' }))
            .finally(() => setLoading(false));
    }, []);

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setSettings(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    const handleDateChange = (e) => {
        setSettings(prev => ({ ...prev, carryoverExpiryDate: e.target.value }));
    };

    const handleLeaveTypeChange = (event) => {
        const { value } = event.target;
        setSettings(prev => ({
            ...prev,
            documentRequiredFor: typeof value === 'string' ? value.split(',') : value,
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSaving(true);
        const notificationPreferences = [
            settings.emailNotifications ? 'email' : null,
            settings.inAppNotifications ? 'in-app' : null
        ].filter(Boolean).join(',');
        const payload = {
            ...settings,
            notificationPreferences,
            documentRequiredFor: (settings.documentRequiredFor || []).join(','),
            emailNotifications: undefined,
            inAppNotifications: undefined,
        };
        try {
            await leaveApi.put('/settings', payload);
            setSnackbar({ open: true, message: 'Settings updated successfully!', severity: 'success' });
        } catch {
            setSnackbar({ open: true, message: 'Failed to update settings', severity: 'error' });
        } finally {
            setSaving(false);
        }
    };

    return (
        <Box sx={{ p: 4 }}>
            <Typography variant="h4" sx={{ mb: 3 }}>System Settings</Typography>
            <Paper sx={{ p: 4, maxWidth: 700, mx: 'auto', boxShadow: 3 }}>
                <form onSubmit={handleSubmit}>
                    <Grid container spacing={3}>
                        <Grid item xs={12} sm={6}>
                            <TextField
                                label="Default Accrual Rate (days/month)"
                                name="accrualRate"
                                type="number"
                                value={settings.accrualRate ?? ""}
                                onChange={handleChange}
                                fullWidth
                                inputProps={{ step: 0.01, min: 0 }}
                                required
                                disabled={loading}
                                size="large"
                            />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <TextField
                                label="Carry-over Cap (days)"
                                name="maxCarryover"
                                type="number"
                                value={settings.maxCarryover ?? ""}
                                onChange={handleChange}
                                fullWidth
                                inputProps={{ min: 0 }}
                                required
                                disabled={loading}
                                size="large"
                            />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <TextField
                                label="Carry-over Expiry"
                                name="carryoverExpiryDate"
                                type="date"
                                value={settings.carryoverExpiryDate ?? "2024-01-31"}
                                onChange={handleDateChange}
                                fullWidth
                                InputLabelProps={{ shrink: true }}
                                helperText="Select expiry date (e.g., 2024-01-31)"
                                required
                                disabled={loading}
                                size="large"
                            />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <FormControlLabel
                                control={<Switch checked={settings.emailNotifications ?? false} onChange={handleChange} name="emailNotifications" />}
                                label="Email Notifications"
                                disabled={loading}
                                sx={{ width: '100%', justifyContent: 'space-between', m: 0 }}
                            />
                            <FormControlLabel
                                control={<Switch checked={settings.inAppNotifications ?? false} onChange={handleChange} name="inAppNotifications" />}
                                label="In-App Notifications"
                                disabled={loading}
                                sx={{ width: '100%', justifyContent: 'space-between', m: 0 }}
                            />
                        </Grid>
                        <Grid item xs={12}>
                            <FormControl fullWidth>
                                <InputLabel id="document-required-label" shrink>
                                    Document Required For
                                </InputLabel>
                                <Select
                                    labelId="document-required-label"
                                    multiple
                                    value={settings.documentRequiredFor || []}
                                    onChange={handleLeaveTypeChange}
                                    input={<OutlinedInput label="Document Required For" />}
                                    renderValue={(selected) => (selected || []).join(', ')}
                                    disabled={loading}
                                    displayEmpty
                                    sx={{ minWidth: 350 }}
                                    placeholder="Select leave types"
                                >
                                    {leaveTypes.map((type) => (
                                        <MenuItem key={type} value={type}>
                                            <Checkbox checked={settings.documentRequiredFor?.indexOf(type) > -1} />
                                            <ListItemText primary={type} />
                                        </MenuItem>
                                    ))}
                                </Select>
                            </FormControl>
                        </Grid>
                        <Grid item xs={12}>
                            <Button type="submit" variant="contained" color="primary" disabled={saving || loading} fullWidth size="large">
                                {saving ? 'Saving...' : 'Save Settings'}
                            </Button>
                        </Grid>
                    </Grid>
                </form>
            </Paper>
            <Snackbar
                open={snackbar.open}
                autoHideDuration={3000}
                onClose={() => setSnackbar({ ...snackbar, open: false })}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
            >
                <Alert severity={snackbar.severity} sx={{ width: '100%' }}>{snackbar.message}</Alert>
            </Snackbar>
        </Box>
    );
};

export default Settings;