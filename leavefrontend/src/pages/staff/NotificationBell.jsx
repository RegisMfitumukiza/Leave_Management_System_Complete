import React, { useState, useEffect } from 'react';
import { Badge, IconButton, Popover, List, ListItemButton, ListItemText, Typography, Box, Button, CircularProgress } from '@mui/material';
import NotificationsIcon from '@mui/icons-material/Notifications';
import { leaveApi } from '@/lib/api';
import { useNavigate } from 'react-router-dom';

const NotificationBell = () => {
    const [anchorEl, setAnchorEl] = useState(null);
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const fetchNotifications = async () => {
        setLoading(true);
        try {
            const [recentRes, unreadRes] = await Promise.all([
                leaveApi.get('/notifications/recent'),
                leaveApi.get('/notifications/unread-count'),
            ]);
            setNotifications(recentRes.data);
            setUnreadCount(unreadRes.data);
        } catch {
            setNotifications([]);
            setUnreadCount(0);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchNotifications();
    }, []);

    const handleOpen = (event) => {
        setAnchorEl(event.currentTarget);
        fetchNotifications();
    };
    const handleClose = () => setAnchorEl(null);

    const handleMarkRead = async (id, link) => {
        await leaveApi.put(`/notifications/mark-read/${id}`);
        fetchNotifications();
        if (link) navigate(link);
    };

    const handleMarkAllRead = async () => {
        await leaveApi.post('/notifications/mark-all-read');
        fetchNotifications();
    };

    return (
        <>
            <IconButton onClick={handleOpen} color="inherit">
                <Badge badgeContent={unreadCount} color="error">
                    <NotificationsIcon />
                </Badge>
            </IconButton>
            <Popover
                open={Boolean(anchorEl)}
                anchorEl={anchorEl}
                onClose={handleClose}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                transformOrigin={{ vertical: 'top', horizontal: 'right' }}
                PaperProps={{ style: { minWidth: 320 } }}
            >
                <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography variant="subtitle1">Notifications</Typography>
                    <Button size="small" onClick={handleMarkAllRead} disabled={unreadCount === 0}>
                        Mark all as read
                    </Button>
                </Box>
                {loading ? (
                    <Box sx={{ p: 2, textAlign: 'center' }}><CircularProgress size={20} /></Box>
                ) : notifications.length === 0 ? (
                    <Box sx={{ p: 2, textAlign: 'center' }}><Typography>No notifications</Typography></Box>
                ) : (
                    <List>
                        {notifications.map((notif) => (
                            <ListItemButton
                                key={notif.id}
                                selected={!notif.read}
                                onClick={() => handleMarkRead(notif.id, notif.link)}
                                sx={{ bgcolor: !notif.read ? '#e3f2fd' : undefined }}
                            >
                                <ListItemText
                                    primary={notif.message}
                                    secondary={notif.type ? notif.type.replace('_', ' ') : ''}
                                />
                            </ListItemButton>
                        ))}
                    </List>
                )}
            </Popover>
        </>
    );
};

export default NotificationBell; 