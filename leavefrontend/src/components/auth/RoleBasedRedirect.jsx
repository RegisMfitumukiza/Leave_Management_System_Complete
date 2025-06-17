import React, { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { authApi } from '@/lib/api';
import { Box, CircularProgress } from '@mui/material';

const RoleBasedRedirect = () => {
    const { user, loading } = useAuth();
    const [error, setError] = useState('');

    useEffect(() => {
        const validateUser = async () => {
            try {
                await authApi.get('/profile');
            } catch (err) {
                setError('Invalid or expired session');
            }
        };
        if (user) {
            validateUser();
        }
    }, [user]);

    if (loading) {
        return (
            <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh">
                <CircularProgress />
            </Box>
        );
    }

    if (error) {
        return <Navigate to="/login" replace />;
    }

    if (!user) {
        return <Navigate to="/login" replace />;
    }

    switch (user.role) {
        case 'ADMIN':
            return <Navigate to="/admin/dashboard" replace />;
        case 'MANAGER':
            return <Navigate to="/manager/dashboard" replace />;
        case 'STAFF':
            return <Navigate to="/staff/dashboard" replace />;
        default:
            return <Navigate to="/login" replace />;
    }
};

export default RoleBasedRedirect; 