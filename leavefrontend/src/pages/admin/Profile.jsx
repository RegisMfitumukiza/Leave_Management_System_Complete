import React, { useEffect, useState } from 'react';
import { Box, Typography, Paper, TextField, Button, Avatar, Grid, Snackbar, Alert, CircularProgress, Divider } from '@mui/material';
import { useAuth } from '../../contexts/AuthContext';
import { authApi, leaveApi } from '@/lib/api';

const CLOUD_NAME = 'daking'; // Replace with your Cloudinary cloud name
const UPLOAD_PRESET = 'unsigned_preset'; // Your Cloudinary unsigned upload preset

const Profile = () => {
    const { user, setUser } = useAuth();
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [avatarUploading, setAvatarUploading] = useState(false);
    const [passwordSaving, setPasswordSaving] = useState(false);
    const [leaveBalances, setLeaveBalances] = useState([]);
    const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
    const [editForm, setEditForm] = useState({ firstName: '', lastName: '', email: '' });
    const [passwordForm, setPasswordForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });

    useEffect(() => {
        const fetchProfile = async () => {
            setLoading(true);
            try {
                const res = await authApi.get('/profile');
                setProfile(res.data);
                setEditForm({
                    firstName: res.data.firstName || '',
                    lastName: res.data.lastName || '',
                    email: res.data.email || '',
                });
                if (res.data.role === 'STAFF') {
                    // Only fetch leave balances for staff members
                    const balancesRes = await leaveApi.get('/leave-balances/me');
                    setLeaveBalances(balancesRes.data);
                } else {
                    setLeaveBalances([]);
                }
            } catch {
                setSnackbar({ open: true, message: 'Failed to load profile', severity: 'error' });
            } finally {
                setLoading(false);
            }
        };
        fetchProfile();
    }, []);

    const handleEditChange = (e) => {
        const { name, value } = e.target;
        setEditForm(prev => ({ ...prev, [name]: value }));
    };

    const handleProfileSave = async (e) => {
        e.preventDefault();
        setSaving(true);
        try {
            const res = await authApi.put(`/profile`, {
                firstName: editForm.firstName,
                lastName: editForm.lastName,
                email: editForm.email,
            });
            setProfile(res.data);
            setSnackbar({ open: true, message: 'Profile updated!', severity: 'success' });
            if (setUser) setUser(res.data);
        } catch {
            setSnackbar({ open: true, message: 'Failed to update profile', severity: 'error' });
        } finally {
            setSaving(false);
        }
    };

    const handleAvatarChange = async (e) => {
        const file = e.target.files[0];
        if (!file) return;
        setAvatarUploading(true);
        try {
            // 1. Upload to Cloudinary
            const formData = new FormData();
            formData.append('file', file);
            formData.append('upload_preset', UPLOAD_PRESET);
            const cloudinaryRes = await fetch(
                `https://api.cloudinary.com/v1_1/${CLOUD_NAME}/image/upload`,
                { method: 'POST', body: formData }
            );
            const cloudinaryData = await cloudinaryRes.json();
            if (!cloudinaryData.secure_url) throw new Error('Upload failed');
            const imageUrl = cloudinaryData.secure_url;
            // 2. Send the image URL to your backend
            const res = await authApi.post('/avatar', { avatarUrl: imageUrl });
            setProfile(prev => ({ ...prev, avatarUrl: res.data.avatarUrl }));
            setSnackbar({ open: true, message: 'Avatar updated!', severity: 'success' });
            if (setUser) setUser(prev => ({ ...prev, avatarUrl: res.data.avatarUrl }));
        } catch {
            setSnackbar({ open: true, message: 'Failed to update avatar', severity: 'error' });
        } finally {
            setAvatarUploading(false);
        }
    };

    const handlePasswordChange = (e) => {
        const { name, value } = e.target;
        setPasswordForm(prev => ({ ...prev, [name]: value }));
    };

    const handlePasswordSave = async (e) => {
        e.preventDefault();
        if (passwordForm.newPassword !== passwordForm.confirmPassword) {
            setSnackbar({ open: true, message: 'Passwords do not match', severity: 'error' });
            return;
        }
        setPasswordSaving(true);
        try {
            await authApi.put('/change-password', {
                currentPassword: passwordForm.currentPassword,
                newPassword: passwordForm.newPassword,
            });
            setSnackbar({ open: true, message: 'Password updated!', severity: 'success' });
            setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
        } catch {
            setSnackbar({ open: true, message: 'Failed to update password', severity: 'error' });
        } finally {
            setPasswordSaving(false);
        }
    };

    // After fetching profile, check if admin
    const isAdmin = profile?.role === 'ADMIN';

    if (loading) return <Box display="flex" justifyContent="center" alignItems="center" minHeight="300px"><CircularProgress /></Box>;
    if (!profile) return null;

    return (
        <Box sx={{ p: 4 }}>
            <Typography variant="h4" sx={{ mb: 3 }}>Profile</Typography>
            <Paper sx={{ p: 3, maxWidth: 600, mb: 4 }}>
                <Grid container spacing={2}>
                    <Grid item xs={12} display="flex" alignItems="center" gap={2}>
                        <Avatar src={profile.avatarUrl} sx={{ width: 64, height: 64 }} />
                        <label htmlFor="avatar-upload">
                            <input
                                id="avatar-upload"
                                type="file"
                                accept="image/*"
                                style={{ display: 'none' }}
                                onChange={handleAvatarChange}
                                disabled={avatarUploading}
                            />
                            <Button variant="outlined" component="span" disabled={avatarUploading}>
                                {avatarUploading ? 'Uploading...' : 'Change Avatar'}
                            </Button>
                        </label>
                    </Grid>
                    <Grid item xs={12}>
                        <form onSubmit={handleProfileSave}>
                            <TextField
                                label="First Name"
                                name="firstName"
                                value={editForm.firstName}
                                onChange={handleEditChange}
                                fullWidth
                                margin="normal"
                                required
                            />
                            <TextField
                                label="Last Name"
                                name="lastName"
                                value={editForm.lastName}
                                onChange={handleEditChange}
                                fullWidth
                                margin="normal"
                                required
                            />
                            <TextField
                                label="Email"
                                name="email"
                                value={editForm.email}
                                onChange={handleEditChange}
                                fullWidth
                                margin="normal"
                                required
                                disabled
                            />
                            <Button type="submit" variant="contained" color="primary" disabled={saving} sx={{ mt: 2 }}>
                                {saving ? 'Saving...' : 'Save Changes'}
                            </Button>
                        </form>
                    </Grid>
                </Grid>
            </Paper>
            <Paper sx={{ p: 3, maxWidth: 600, mb: 4 }}>
                <Typography variant="h6" sx={{ mb: 2 }}>Change Password</Typography>
                <form onSubmit={handlePasswordSave}>
                    <TextField
                        label="Current Password"
                        name="currentPassword"
                        type="password"
                        value={passwordForm.currentPassword}
                        onChange={handlePasswordChange}
                        fullWidth
                        margin="normal"
                        required
                    />
                    <TextField
                        label="New Password"
                        name="newPassword"
                        type="password"
                        value={passwordForm.newPassword}
                        onChange={handlePasswordChange}
                        fullWidth
                        margin="normal"
                        required
                    />
                    <TextField
                        label="Confirm New Password"
                        name="confirmPassword"
                        type="password"
                        value={passwordForm.confirmPassword}
                        onChange={handlePasswordChange}
                        fullWidth
                        margin="normal"
                        required
                    />
                    <Button type="submit" variant="contained" color="primary" disabled={passwordSaving} sx={{ mt: 2 }}>
                        {passwordSaving ? 'Saving...' : 'Change Password'}
                    </Button>
                </form>
            </Paper>
            {profile?.role === 'STAFF' && (
                <Paper sx={{ p: 3, maxWidth: 600 }}>
                    <Typography variant="h6" sx={{ mb: 2 }}>Leave Balances</Typography>
                    {leaveBalances.length === 0 ? (
                        <Typography>No leave balances found.</Typography>
                    ) : (
                        <Grid container spacing={2}>
                            {leaveBalances.map(balance => (
                                <Grid item xs={12} sm={6} key={balance.id}>
                                    <Paper sx={{ p: 2 }}>
                                        <Typography variant="subtitle1">{balance.leaveType.name}</Typography>
                                        <Typography variant="body2">Total: {balance.totalDays}</Typography>
                                        <Typography variant="body2">Used: {balance.usedDays}</Typography>
                                        <Typography variant="body2">Remaining: {balance.remainingDays}</Typography>
                                        {balance.carriedOverDays > 0 && (
                                            <Typography variant="body2" color="warning.main">Carried Over: {balance.carriedOverDays}</Typography>
                                        )}
                                    </Paper>
                                </Grid>
                            ))}
                        </Grid>
                    )}
                </Paper>
            )}
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

export default Profile; 