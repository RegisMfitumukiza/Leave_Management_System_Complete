import React from 'react';
import { Box, Typography, Button } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { authApi } from '@/lib/api';

// ... rest of imports

const Unauthorized = () => {
    // ... state declarations

    const getDashboardPath = () => {
        switch (user?.role) {
            case 'ADMIN':
                return '/admin/dashboard';
            case 'MANAGER':
                return '/manager/dashboard';
            case 'STAFF':
                return '/employee/dashboard';
            default:
                return '/login';
        }
    };

    // ... rest of the component
} 