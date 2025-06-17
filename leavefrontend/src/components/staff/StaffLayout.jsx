import React from 'react';
import { Box, AppBar, Toolbar, Typography, IconButton, Button, Avatar, Tooltip } from '@mui/material';
import { Logout as LogoutIcon } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import StaffSidebar from './StaffSidebar';
import NotificationBell from '../../pages/staff/NotificationBell';

const StaffLayout = ({ children }) => {
    const navigate = useNavigate();
    const { user, logout } = useAuth();

    const handleLogout = () => {
        logout();
        navigate('/auth/login');
    };

    return (
        <Box sx={{ display: 'flex' }}>
            <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
                <Toolbar>
                    <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
                        Leave Management System
                    </Typography>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <NotificationBell />
                        <Tooltip title={`${user?.firstName ?? ''} ${user?.lastName ?? ''}`}>
                            <Avatar
                                src={user?.avatarUrl}
                                alt={user?.firstName}
                                sx={{ width: 32, height: 32, mr: 2, bgcolor: 'primary.main', fontWeight: 700 }}
                            >
                                {user?.firstName?.[0]}
                                {user?.lastName?.[0]}
                            </Avatar>
                        </Tooltip>
                        <Button
                            color="inherit"
                            startIcon={<LogoutIcon />}
                            onClick={handleLogout}
                        >
                            Logout
                        </Button>
                    </Box>
                </Toolbar>
            </AppBar>
            <StaffSidebar />
            <Box
                component="main"
                sx={{
                    flexGrow: 1,
                    p: 3,
                    width: { sm: `calc(100% - 220px)` },
                    ml: { sm: '220px' },
                    mt: '64px'
                }}
            >
                {children}
            </Box>
        </Box>
    );
};

export default StaffLayout; 