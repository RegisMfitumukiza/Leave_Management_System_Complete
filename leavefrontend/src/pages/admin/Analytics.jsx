import React, { useState, useEffect } from 'react';
import { Box, Typography, Paper, Grid, CircularProgress, Alert, FormControl, InputLabel, Select, MenuItem } from '@mui/material';
import { leaveApi } from '@/lib/api';
import dayjs from 'dayjs';

const Analytics = () => {
    // ... state declarations

    useEffect(() => {
        fetchAnalytics();
    }, [timeRange]);

    const fetchAnalytics = async () => {
        setLoading(true);
        setError('');
        try {
            const res = await leaveApi.get('/analytics', {
                params: { timeRange }
            });
            setAnalytics(res.data);
        } catch (err) {
            setError('Failed to load analytics data');
            console.error('Analytics fetch error:', err);
        } finally {
            setLoading(false);
        }
    };

    // ... rest of the component
}; 