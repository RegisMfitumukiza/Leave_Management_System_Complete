import React, { useState } from 'react';
import {
    Box,
    Paper,
    Typography,
    Button,
    TextField,
    Link,
    CircularProgress,
    Alert,
    useTheme,
    Grid,
    Divider
} from '@mui/material';
import GoogleIcon from '@mui/icons-material/Google';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import { authApi } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';

const LoginPage = () => {
    const theme = useTheme();
    const navigate = useNavigate();
    const { login } = useAuth();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [form, setForm] = useState({
        email: '',
        password: ''
    });

    const handleChange = (e) => {
        const { name, value } = e.target;
        setForm(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        try {
            const res = await authApi.post('/login', {
                email: form.email,
                password: form.password
            });
            login(res.data);
            navigate('/auth/redirect');
        } catch (err) {
            const errData = err.response?.data;
            if (typeof errData === 'string') {
                setError(errData);
            } else if (errData && (errData.error || errData.message)) {
                setError(errData.message || errData.error);
            } else {
                setError('Failed to login');
            }
        } finally {
            setLoading(false);
        }
    };

    const handleGoogleLogin = async () => {
        setLoading(true);
        setError('');
        try {
            const baseUrl = import.meta.env.VITE_AUTH_SERVICE_URL.replace('/api/auth', '');
            window.location.href = `${baseUrl}/api/oauth2/authorization/google`;
        } catch (err) {
            setError('Failed to initiate Google login');
            setLoading(false);
        }
    };

    return (
        <Box
            sx={{
                minHeight: '100vh',
                width: '100vw',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                bgcolor: 'background.default',
                background: 'linear-gradient(135deg, #42a5f5 0%, #1976d2 100%)',
            }}
        >
            <Paper
                elevation={6}
                sx={{
                    p: { xs: 3, sm: 4 },
                    borderRadius: 2,
                    background: 'rgba(255, 255, 255, 0.95)',
                    backdropFilter: 'blur(10px)',
                    boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
                    width: '100%',
                    maxWidth: 420,
                }}
            >
                <Box
                    sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        mb: 4
                    }}
                >
                    <Typography
                        variant="h4"
                        component="h1"
                        gutterBottom
                        sx={{
                            fontWeight: 700,
                            color: theme.palette.primary.main,
                            textAlign: 'center'
                        }}
                    >
                        Welcome Back
                    </Typography>
                    <Typography
                        variant="body1"
                        color="text.secondary"
                        sx={{ textAlign: 'center', mb: 1 }}
                    >
                        Sign in to your account
                    </Typography>
                </Box>

                {error && (
                    <Alert
                        severity="error"
                        sx={{
                            mb: 3,
                            borderRadius: 1,
                            '& .MuiAlert-icon': {
                                alignItems: 'center'
                            }
                        }}
                    >
                        {error}
                    </Alert>
                )}

                <Box component="form" onSubmit={handleSubmit} sx={{ width: '100%' }}>
                    <TextField
                        fullWidth
                        label="Email"
                        name="email"
                        value={form.email}
                        onChange={handleChange}
                        required
                        margin="normal"
                        autoFocus
                        sx={{
                            '& .MuiOutlinedInput-root': {
                                borderRadius: 1.5,
                                '&:hover fieldset': {
                                    borderColor: theme.palette.primary.main
                                }
                            }
                        }}
                    />
                    <TextField
                        fullWidth
                        label="Password"
                        name="password"
                        type="password"
                        value={form.password}
                        onChange={handleChange}
                        required
                        margin="normal"
                        autoComplete="current-password"
                        sx={{
                            '& .MuiOutlinedInput-root': {
                                borderRadius: 1.5,
                                '&:hover fieldset': {
                                    borderColor: theme.palette.primary.main
                                }
                            }
                        }}
                    />
                    <Button
                        fullWidth
                        type="submit"
                        variant="contained"
                        size="large"
                        disabled={loading}
                        sx={{
                            mt: 3,
                            mb: 2,
                            py: 1.5,
                            borderRadius: 1.5,
                            textTransform: 'none',
                            fontSize: '1.1rem',
                            fontWeight: 600,
                            boxShadow: '0 4px 12px rgba(0, 0, 0, 0.1)',
                            '&:hover': {
                                boxShadow: '0 6px 16px rgba(0, 0, 0, 0.15)'
                            }
                        }}
                    >
                        {loading ? <CircularProgress size={24} /> : 'Sign In'}
                    </Button>
                </Box>

                <Divider sx={{ my: 3 }}>
                    <Typography variant="body2" color="text.secondary">
                        OR
                    </Typography>
                </Divider>

                <Button
                    fullWidth
                    variant="outlined"
                    startIcon={<GoogleIcon />}
                    onClick={handleGoogleLogin}
                    disabled={loading}
                    sx={{
                        py: 1.5,
                        borderRadius: 1.5,
                        textTransform: 'none',
                        fontSize: '1.1rem',
                        fontWeight: 600,
                        borderColor: theme.palette.grey[300],
                        '&:hover': {
                            borderColor: theme.palette.grey[400],
                            backgroundColor: theme.palette.grey[50]
                        }
                    }}
                >
                    Continue with Google
                </Button>

                <Box sx={{ mt: 4, textAlign: 'center' }}>
                    <Typography variant="body2" color="text.secondary">
                        Don't have an account?{' '}
                        <Link
                            component={RouterLink}
                            to="/auth/register"
                            variant="body2"
                            sx={{
                                color: theme.palette.primary.main,
                                fontWeight: 600,
                                textDecoration: 'none',
                                '&:hover': {
                                    textDecoration: 'underline'
                                }
                            }}
                        >
                            Sign up
                        </Link>
                    </Typography>
                    <Box sx={{ mt: 1 }}>
                        <Link
                            component={RouterLink}
                            to="/auth/forgot-password"
                            variant="body2"
                            sx={{
                                color: theme.palette.primary.main,
                                textDecoration: 'none',
                                '&:hover': {
                                    textDecoration: 'underline'
                                }
                            }}
                        >
                            Forgot password?
                        </Link>
                    </Box>
                </Box>
            </Paper>
        </Box>
    );
};

export default LoginPage; 