import React, { useEffect, useState } from 'react';
import { Box, Typography, Paper, Grid, CircularProgress, Alert, FormControl, InputLabel, Select, MenuItem, Tabs, Tab } from '@mui/material';
import { leaveApi } from '@/lib/api';
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
    PieChart, Pie, Cell, LineChart, Line
} from 'recharts';

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884d8'];

const LeaveAnalytics = () => {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [year, setYear] = useState(new Date().getFullYear());
    const [interval, setInterval] = useState('MONTHLY');
    const [tab, setTab] = useState(0);

    // Analytics data states
    const [deptDistribution, setDeptDistribution] = useState(null);
    const [usageTrends, setUsageTrends] = useState(null);
    const [balanceAlerts, setBalanceAlerts] = useState([]);
    const [ytdConsumption, setYtdConsumption] = useState(null);
    const [carryoverStats, setCarryoverStats] = useState(null);

    useEffect(() => {
        const fetchData = async () => {
            setLoading(true);
            setError('');
            try {
                const [deptRes, trendsRes, alertsRes, ytdRes, carryoverRes] = await Promise.all([
                    leaveApi.get(`/leave-analytics/department-distribution?year=${year}`),
                    leaveApi.get(`/leave-analytics/usage-trends?year=${year}&interval=${interval}`),
                    leaveApi.get(`/leave-analytics/balance-alerts`),
                    leaveApi.get(`/leave-analytics/ytd-consumption?year=${year}`),
                    leaveApi.get(`/leave-analytics/carryover-stats?year=${year}`)
                ]);

                setDeptDistribution(deptRes.data);
                setUsageTrends(trendsRes.data);
                setBalanceAlerts(alertsRes.data);
                setYtdConsumption(ytdRes.data);
                setCarryoverStats(carryoverRes.data);
            } catch (err) {
                setError('Failed to load analytics data');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, [year, interval]);

    const handleTabChange = (event, newValue) => {
        setTab(newValue);
    };

    if (loading) {
        return (
            <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
                <CircularProgress />
            </Box>
        );
    }

    if (error) {
        return <Alert severity="error">{error}</Alert>;
    }

    return (
        <Box sx={{ p: 3 }}>
            <Typography variant="h4" sx={{ mb: 3 }}>Leave Analytics</Typography>

            <Box sx={{ mb: 3, display: 'flex', gap: 2 }}>
                <FormControl sx={{ minWidth: 120 }}>
                    <InputLabel>Year</InputLabel>
                    <Select
                        value={year}
                        label="Year"
                        onChange={(e) => setYear(e.target.value)}
                    >
                        {[year - 1, year, year + 1].map(y => (
                            <MenuItem key={y} value={y}>{y}</MenuItem>
                        ))}
                    </Select>
                </FormControl>

                <FormControl sx={{ minWidth: 120 }}>
                    <InputLabel>Interval</InputLabel>
                    <Select
                        value={interval}
                        label="Interval"
                        onChange={(e) => setInterval(e.target.value)}
                    >
                        <MenuItem value="MONTHLY">Monthly</MenuItem>
                        <MenuItem value="QUARTERLY">Quarterly</MenuItem>
                    </Select>
                </FormControl>
            </Box>

            <Tabs value={tab} onChange={handleTabChange} sx={{ mb: 3 }}>
                <Tab label="Department Distribution" />
                <Tab label="Usage Trends" />
                <Tab label="Balance Alerts" />
                <Tab label="YTD Consumption" />
                <Tab label="Carryover Stats" />
            </Tabs>

            {tab === 0 && deptDistribution && (
                <Grid container spacing={3}>
                    <Grid item xs={12} md={6}>
                        <Paper sx={{ p: 2 }}>
                            <Typography variant="h6" sx={{ mb: 2 }}>Department Distribution</Typography>
                            <ResponsiveContainer width="100%" height={300}>
                                <BarChart data={Object.entries(deptDistribution).map(([dept, stats]) => ({
                                    name: dept,
                                    total: stats.totalDays,
                                    used: stats.usedDays,
                                    remaining: stats.remainingDays
                                }))}>
                                    <CartesianGrid strokeDasharray="3 3" />
                                    <XAxis dataKey="name" />
                                    <YAxis />
                                    <Tooltip />
                                    <Legend />
                                    <Bar dataKey="total" name="Total Days" fill="#8884d8" />
                                    <Bar dataKey="used" name="Used Days" fill="#82ca9d" />
                                    <Bar dataKey="remaining" name="Remaining Days" fill="#ffc658" />
                                </BarChart>
                            </ResponsiveContainer>
                        </Paper>
                    </Grid>
                </Grid>
            )}

            {tab === 1 && usageTrends && (
                <Grid container spacing={3}>
                    <Grid item xs={12}>
                        <Paper sx={{ p: 2 }}>
                            <Typography variant="h6" sx={{ mb: 2 }}>Leave Usage Trends</Typography>
                            <ResponsiveContainer width="100%" height={300}>
                                <LineChart data={Object.entries(usageTrends).map(([period, days]) => ({
                                    period,
                                    days
                                }))}>
                                    <CartesianGrid strokeDasharray="3 3" />
                                    <XAxis dataKey="period" />
                                    <YAxis />
                                    <Tooltip />
                                    <Legend />
                                    <Line type="monotone" dataKey="days" name="Days Used" stroke="#8884d8" />
                                </LineChart>
                            </ResponsiveContainer>
                        </Paper>
                    </Grid>
                </Grid>
            )}

            {tab === 2 && (
                <Grid container spacing={3}>
                    <Grid item xs={12}>
                        <Paper sx={{ p: 2 }}>
                            <Typography variant="h6" sx={{ mb: 2 }}>Balance Alerts</Typography>
                            {balanceAlerts.length === 0 ? (
                                <Alert severity="success">No balance alerts at this time.</Alert>
                            ) : (
                                <Box sx={{ overflowX: 'auto' }}>
                                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                                        <thead>
                                            <tr>
                                                <th style={{ padding: '8px', textAlign: 'left' }}>User</th>
                                                <th style={{ padding: '8px', textAlign: 'left' }}>Department</th>
                                                <th style={{ padding: '8px', textAlign: 'left' }}>Leave Type</th>
                                                <th style={{ padding: '8px', textAlign: 'left' }}>Remaining Days</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {balanceAlerts.map((alert, index) => (
                                                <tr key={index}>
                                                    <td style={{ padding: '8px' }}>{alert.userName}</td>
                                                    <td style={{ padding: '8px' }}>{alert.department}</td>
                                                    <td style={{ padding: '8px' }}>{alert.leaveType}</td>
                                                    <td style={{ padding: '8px' }}>{alert.remainingDays}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </Box>
                            )}
                        </Paper>
                    </Grid>
                </Grid>
            )}

            {tab === 3 && ytdConsumption && (
                <Grid container spacing={3}>
                    <Grid item xs={12} md={6}>
                        <Paper sx={{ p: 2 }}>
                            <Typography variant="h6" sx={{ mb: 2 }}>YTD Consumption by Leave Type</Typography>
                            <ResponsiveContainer width="100%" height={300}>
                                <PieChart>
                                    <Pie
                                        data={Object.entries((ytdConsumption && ytdConsumption.byLeaveType) || {}).map(([type, days]) => ({
                                            name: type,
                                            value: days
                                        }))}
                                        dataKey="value"
                                        nameKey="name"
                                        cx="50%"
                                        cy="50%"
                                        outerRadius={80}
                                        label
                                    >
                                        {Object.entries((ytdConsumption && ytdConsumption.byLeaveType) || {}).map((entry, index) => (
                                            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                        ))}
                                    </Pie>
                                    <Tooltip />
                                    <Legend />
                                </PieChart>
                            </ResponsiveContainer>
                        </Paper>
                    </Grid>
                    <Grid item xs={12} md={6}>
                        <Paper sx={{ p: 2 }}>
                            <Typography variant="h6" sx={{ mb: 2 }}>Summary</Typography>
                            <Typography>Total Applications: {ytdConsumption.totalApplications}</Typography>
                            <Typography>Total Days: {ytdConsumption.totalDays}</Typography>
                        </Paper>
                    </Grid>
                </Grid>
            )}

            {tab === 4 && carryoverStats && (
                <Grid container spacing={3}>
                    <Grid item xs={12} md={6}>
                        <Paper sx={{ p: 2 }}>
                            <Typography variant="h6" sx={{ mb: 2 }}>Carryover by Leave Type</Typography>
                            <ResponsiveContainer width="100%" height={300}>
                                <BarChart data={Object.entries((carryoverStats && carryoverStats.byLeaveType) || {}).map(([type, days]) => ({
                                    name: type,
                                    days
                                }))}>
                                    <CartesianGrid strokeDasharray="3 3" />
                                    <XAxis dataKey="name" />
                                    <YAxis />
                                    <Tooltip />
                                    <Legend />
                                    <Bar dataKey="days" name="Carried Over Days" fill="#8884d8" />
                                </BarChart>
                            </ResponsiveContainer>
                        </Paper>
                    </Grid>
                    <Grid item xs={12} md={6}>
                        <Paper sx={{ p: 2 }}>
                            <Typography variant="h6" sx={{ mb: 2 }}>Carryover Utilization</Typography>
                            <ResponsiveContainer width="100%" height={300}>
                                <BarChart data={Object.entries((carryoverStats && carryoverStats.utilizationRate) || {}).map(([type, rate]) => ({
                                    name: type,
                                    rate: (rate * 100).toFixed(1)
                                }))}>
                                    <CartesianGrid strokeDasharray="3 3" />
                                    <XAxis dataKey="name" />
                                    <YAxis />
                                    <Tooltip />
                                    <Legend />
                                    <Bar dataKey="rate" name="Utilization Rate (%)" fill="#82ca9d" />
                                </BarChart>
                            </ResponsiveContainer>
                        </Paper>
                    </Grid>
                </Grid>
            )}
        </Box>
    );
};

export default LeaveAnalytics; 