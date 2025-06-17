import { AppBar, Toolbar, Typography, Box, IconButton, Avatar, Badge, Menu, MenuItem, ListItemText, CircularProgress, Button } from '@mui/material';
import NotificationsIcon from '@mui/icons-material/Notifications';
import LogoutIcon from '@mui/icons-material/Logout';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';

const Topbar = ({
    title = 'Leave Management System',
    showNotifications = false,
    notifications = [],
    unreadCount = 0,
    onNotificationsClick = () => { },
    onMarkAllRead = () => { },
    onMarkRead = () => { },
    loading = false,
    avatarLetter = 'A',
    avatarUrl = '',
    anchorEl = null,
    onCloseNotifications = () => { },
}) => {
    const { logout } = useAuth();
    const navigate = useNavigate();
    const handleLogout = () => {
        logout();
        navigate('/auth/login');
    };
    return (
        <AppBar position="static" elevation={0} sx={{ background: '#fff', color: '#1976d2', zIndex: 1201 }}>
            <Toolbar>
                <Typography variant="h6" sx={{ flexGrow: 1, fontWeight: 700 }}>
                    {title}
                </Typography>
                {showNotifications && (
                    <>
                        <IconButton color="inherit" onClick={onNotificationsClick}>
                            <Badge badgeContent={unreadCount} color="error">
                                <NotificationsIcon />
                            </Badge>
                        </IconButton>
                        <Menu
                            anchorEl={anchorEl}
                            open={Boolean(anchorEl)}
                            onClose={onCloseNotifications}
                            PaperProps={{ style: { minWidth: 320 } }}
                        >
                            <MenuItem disabled divider>
                                <Box sx={{ display: 'flex', justifyContent: 'space-between', width: '100%' }}>
                                    <span>Notifications</span>
                                    <Button size="small" onClick={onMarkAllRead} disabled={unreadCount === 0}>
                                        Mark all as read
                                    </Button>
                                </Box>
                            </MenuItem>
                            {loading ? (
                                <MenuItem><CircularProgress size={20} /></MenuItem>
                            ) : notifications.length === 0 ? (
                                <MenuItem><ListItemText primary="No notifications" /></MenuItem>
                            ) : notifications.map((notif) => (
                                <MenuItem
                                    key={notif.id}
                                    onClick={() => onMarkRead(notif.id)}
                                    selected={!notif.read}
                                    sx={{ bgcolor: !notif.read ? '#e3f2fd' : undefined }}
                                >
                                    <ListItemText
                                        primary={notif.message}
                                        secondary={notif.createdAt}
                                    />
                                </MenuItem>
                            ))}
                        </Menu>
                    </>
                )}
                <Box>
                    <IconButton>
                        {avatarUrl ? (
                            <Avatar sx={{ bgcolor: '#1976d2' }} src={avatarUrl} />
                        ) : (
                            <Avatar sx={{ bgcolor: '#1976d2' }}>{avatarLetter}</Avatar>
                        )}
                    </IconButton>
                    <IconButton onClick={handleLogout} sx={{ ml: 1 }}>
                        <LogoutIcon />
                    </IconButton>
                </Box>
            </Toolbar>
        </AppBar>
    );
};

export default Topbar; 