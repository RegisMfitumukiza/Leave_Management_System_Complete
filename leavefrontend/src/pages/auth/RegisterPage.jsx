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
    Grid,
    Divider,
    useTheme
} from '@mui/material';
import GoogleIcon from '@mui/icons-material/Google';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import { authApi } from '@/lib/api';

const RegisterPage = () => {
    const theme = useTheme();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [form, setForm] = useState({
        firstName: '',
        lastName: '',
        email: '',
        password: '',
        confirmPassword: ''
    });

    const handleChange = (e) => {
        const { name, value } = e.target;
        setForm(prev => ({ ...prev, [name]: value }));
    };

    const validateEmail = (email) => {
        if (!email.includes('@')) {
            return 'Email must contain @';
        }
        if (!email.toLowerCase().includes('.staff@')) {
            return 'Staff email must contain .staff@';
        }
        return '';
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (form.password !== form.confirmPassword) {
            setError('Passwords do not match');
            return;
        }
        setLoading(true);
        setError('');
        try {
            await authApi.post('/register', {
                firstName: form.firstName,
                lastName: form.lastName,
                email: form.email,
                password: form.password
            });
            navigate('/auth/login', { state: { message: 'Registration successful! Please login.' } });
        } catch (err) {
            setError(err.response?.data?.message || err.response?.data || 'Failed to register');
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
                minWidth: '100vw',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: `linear-gradient(135deg, ${theme.palette.primary.light} 0%, ${theme.palette.primary.main} 100%)`,
                py: 4,
                px: 2
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
                    maxWidth: 520,
                    width: '100%'
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
                        Register
                    </Typography>
                    <Typography
                        variant="body1"
                        color="text.secondary"
                        sx={{ textAlign: 'center', mb: 1 }}
                    >
                        Join our leave management system
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
                    <Grid container spacing={2}>
                        <Grid item xs={12} sm={6}>
                            <TextField
                                fullWidth
                                label="First Name"
                                name="firstName"
                                value={form.firstName}
                                onChange={handleChange}
                                required
                                placeholder="Enter your first name"
                                sx={{
                                    '& .MuiOutlinedInput-root': {
                                        borderRadius: 1.5,
                                        '&:hover fieldset': {
                                            borderColor: theme.palette.primary.main
                                        }
                                    }
                                }}
                            />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <TextField
                                fullWidth
                                label="Last Name"
                                name="lastName"
                                value={form.lastName}
                                onChange={handleChange}
                                required
                                placeholder="Enter your last name"
                                sx={{
                                    '& .MuiOutlinedInput-root': {
                                        borderRadius: 1.5,
                                        '&:hover fieldset': {
                                            borderColor: theme.palette.primary.main
                                        }
                                    }
                                }}
                            />
                        </Grid>
                    </Grid>

                    <TextField
                        fullWidth
                        label="Email"
                        name="email"
                        type="email"
                        value={form.email}
                        onChange={handleChange}
                        required
                        margin="normal"
                        autoComplete="email"
                        placeholder="Enter your email (must contain .staff@)"
                        error={!!error && error.includes('email')}
                        helperText={error && error.includes('email') ? error : 'Email must contain .staff@'}
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
                        autoComplete="new-password"
                        placeholder="Enter your password"
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
                        label="Confirm Password"
                        name="confirmPassword"
                        type="password"
                        value={form.confirmPassword}
                        onChange={handleChange}
                        required
                        margin="normal"
                        autoComplete="new-password"
                        placeholder="Confirm your password"
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
                        {loading ? <CircularProgress size={24} /> : 'Register'}
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
                    type="button"
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
                        Already have an account?{' '}
                        <Link
                            component={RouterLink}
                            to="/auth/login"
                            sx={{
                                color: theme.palette.primary.main,
                                fontWeight: 600,
                                textDecoration: 'none',
                                '&:hover': {
                                    textDecoration: 'underline'
                                }
                            }}
                        >
                            Sign in
                        </Link>
                    </Typography>
                </Box>
            </Paper>
        </Box>
    );
};

export default RegisterPage; 