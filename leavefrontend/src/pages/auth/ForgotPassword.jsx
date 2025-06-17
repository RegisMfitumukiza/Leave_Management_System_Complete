import React, { useState } from 'react';
import { Box, Typography, Paper, TextField, Button, CircularProgress, Alert, Link } from '@mui/material';
import { authApi } from '@/lib/api';
import { useNavigate } from 'react-router-dom';

const ForgotPassword = () => {
    const [form, setForm] = useState({ email: '' });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        try {
            await authApi.post('/forgot-password', {
                email: form.email
            });
            setSnackbar({ open: true, message: 'Password reset instructions sent to your email', severity: 'success' });
            setTimeout(() => navigate('/login'), 2000);
        } catch (err) {
            setError(err.response?.data || 'Failed to process password reset request');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            {/* ... rest of the component */}
        </div>
    );
};

export default ForgotPassword; 