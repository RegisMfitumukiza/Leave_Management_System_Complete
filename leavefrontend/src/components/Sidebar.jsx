import React from 'react';
import { Drawer, List, ListItem, ListItemIcon, ListItemText, Toolbar, Box, Badge } from '@mui/material';
import ListItemButton from '@mui/material/ListItemButton';
import DashboardIcon from '@mui/icons-material/Dashboard';
import PeopleIcon from '@mui/icons-material/People';
import BusinessIcon from '@mui/icons-material/Business';
import EventIcon from '@mui/icons-material/Event';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import AssessmentIcon from '@mui/icons-material/Assessment';
import SettingsIcon from '@mui/icons-material/Settings';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import { useLocation, useNavigate } from 'react-router-dom';

const drawerWidth = 220;

const navItems = [
    { label: 'Dashboard', icon: <DashboardIcon />, path: '/admin/dashboard' },
    { label: 'User Management', icon: <PeopleIcon />, path: '/admin/users' },
    { label: 'Department Management', icon: <BusinessIcon />, path: '/admin/departments' },
    { label: 'Leave Types', icon: <EventIcon />, path: '/admin/leave-types' },
    { label: 'Holidays', icon: <CalendarMonthIcon />, path: '/admin/holidays' },
    { label: 'Reports', icon: <AssessmentIcon />, path: '/admin/reports' },
    { label: 'Team Calendar', icon: <EventIcon />, path: '/admin/team-calendar' },
    { label: 'Leave Analytics', icon: <AssessmentIcon />, path: '/admin/leave-analytics' },
    { label: 'Settings', icon: <SettingsIcon />, path: '/admin/settings' },
    { label: 'Profile', icon: <AccountCircleIcon />, path: '/admin/profile' },
];

const Sidebar = ({ items = [], selected, onSelect, notificationCount = 0 }) => {
    const navigate = useNavigate();
    const location = useLocation();
    return (
        <Drawer
            variant="permanent"
            sx={{
                width: drawerWidth,
                flexShrink: 0,
                [`& .MuiDrawer-paper`]: { width: drawerWidth, boxSizing: 'border-box', background: '#1976d2', color: '#fff' },
            }}
        >
            <Toolbar />
            <Box sx={{ overflow: 'auto' }}>
                <List>
                    {items.map((item) => (
                        <ListItem key={item.label} disablePadding>
                            <ListItemButton
                                selected={selected ? selected === item.path : location.pathname === item.path}
                                onClick={() => {
                                    if (onSelect) onSelect(item.path);
                                    navigate(item.path);
                                }}
                            >
                                <ListItemIcon sx={{ color: '#fff' }}>
                                    {item.label === 'Notifications' && notificationCount > 0 ? (
                                        <Badge badgeContent={notificationCount} color="error">
                                            {item.icon}
                                        </Badge>
                                    ) : (
                                        item.icon
                                    )}
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

export default Sidebar; 