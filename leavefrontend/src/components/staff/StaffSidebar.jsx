import React from 'react';
import { Drawer, List, ListItem, ListItemIcon, ListItemText, Toolbar, Box } from '@mui/material';
import ListItemButton from '@mui/material/ListItemButton';
import DashboardIcon from '@mui/icons-material/Dashboard';
import EventNoteIcon from '@mui/icons-material/EventNote';
import HistoryIcon from '@mui/icons-material/History';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import { useLocation, useNavigate } from 'react-router-dom';

const drawerWidth = 220;

const navItems = [
    { label: 'Dashboard', icon: <DashboardIcon />, path: '/staff/dashboard' },
    { label: 'Apply Leave', icon: <EventNoteIcon />, path: '/staff/apply-leave' },
    { label: 'Leave History', icon: <HistoryIcon />, path: '/staff/leave-history' },
    { label: 'Profile', icon: <AccountCircleIcon />, path: '/staff/profile' },
];

const StaffSidebar = () => {
    const navigate = useNavigate();
    const location = useLocation();

    return (
        <Drawer
            variant="permanent"
            sx={{
                width: drawerWidth,
                flexShrink: 0,
                '& .MuiDrawer-paper': {
                    width: drawerWidth,
                    boxSizing: 'border-box',
                },
            }}
        >
            <Toolbar />
            <Box sx={{ overflow: 'auto' }}>
                <List>
                    {navItems.map((item) => (
                        <ListItem key={item.path} disablePadding>
                            <ListItemButton
                                selected={location.pathname === item.path}
                                onClick={() => navigate(item.path)}
                            >
                                <ListItemIcon>
                                    {item.icon}
                                </ListItemIcon>
                                <ListItemText primary={item.label} />
                            </ListItemButton>
                        </ListItem>
                    ))}
                </List>
            </Box>
        </Drawer>
    );
};

export default StaffSidebar; 