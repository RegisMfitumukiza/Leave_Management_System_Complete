import React, { useState } from 'react';
import { Box, Typography, Paper, TextField, Button, CircularProgress, Alert, Link } from '@mui/material';
import { authApi } from '@/lib/api';
import { useNavigate, useSearchParams } from 'react-router-dom';

const ResetPassword = () => {
    // ... state declarations

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        try {
            if (form.password !== form.confirmPassword) {
                throw new Error('Passwords do not match');
            }
            await authApi.post('/reset-password', {
                token,
                password: form.password
            });
            setSnackbar({ open: true, message: 'Password reset successful! Please login.', severity: 'success' });
            setTimeout(() => navigate('/login'), 2000);
        } catch (err) {
            setError(err.response?.data || err.message || 'Failed to reset password');
        } finally {
            setLoading(false);
        }
    };

    // ... rest of the component
}; 