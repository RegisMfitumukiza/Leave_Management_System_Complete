import React from 'react';
import { Drawer, List, ListItem, ListItemIcon, ListItemText, Divider, Box, Typography } from '@mui/material';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { authApi } from '@/lib/api';

// ... rest of imports

const Sidebar = () => {
    // ... state declarations

    const getMenuItems = () => {
        const items = [];
        if (user?.role === 'ADMIN') {
            items.push(
                { text: 'Dashboard', icon: <DashboardIcon />, path: '/admin/dashboard' },
                { text: 'User Management', icon: <PeopleIcon />, path: '/admin/users' },
                { text: 'Department Management', icon: <BusinessIcon />, path: '/admin/departments' },
                { text: 'Leave Types', icon: <CategoryIcon />, path: '/admin/leave-types' },
                { text: 'Holidays', icon: <EventIcon />, path: '/admin/holidays' },
                { text: 'Reports', icon: <AssessmentIcon />, path: '/admin/reports' },
                { text: 'Analytics', icon: <AnalyticsIcon />, path: '/admin/analytics' }
            );
        } else if (user?.role === 'MANAGER') {
            items.push(
                { text: 'Dashboard', icon: <DashboardIcon />, path: '/manager/dashboard' },
                { text: 'Leave Approvals', icon: <ApprovalIcon />, path: '/manager/approvals' },
                { text: 'Team Calendar', icon: <CalendarMonthIcon />, path: '/manager/team-calendar' },
                { text: 'Reports', icon: <AssessmentIcon />, path: '/manager/reports' }
            );
        } else {
            items.push(
                { text: 'Dashboard', icon: <DashboardIcon />, path: '/employee/dashboard' },
                { text: 'Apply Leave', icon: <AddIcon />, path: '/employee/apply-leave' },
                { text: 'Leave History', icon: <HistoryIcon />, path: '/employee/leave-history' },
                { text: 'Leave Balance', icon: <AccountBalanceIcon />, path: '/employee/leave-balance' }
            );
        }
        return items;
    };

    // ... rest of the component
} 