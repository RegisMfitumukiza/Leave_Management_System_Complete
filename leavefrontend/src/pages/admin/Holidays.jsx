import React, { useEffect, useState } from 'react';
import { Box, Typography, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Button, IconButton, Dialog, DialogTitle, DialogContent, DialogActions, TextField, CircularProgress, Snackbar, Alert } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { leaveApi } from '@/lib/api';
import dayjs from 'dayjs';

const Holidays = () => {
    const [holidays, setHolidays] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [openAdd, setOpenAdd] = useState(false);
    const [openEdit, setOpenEdit] = useState(false);
    const [openDelete, setOpenDelete] = useState(false);
    const [selectedHoliday, setSelectedHoliday] = useState(null);
    const [form, setForm] = useState({ name: '', date: '', description: '', isPublic: true });
    const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
    const [formLoading, setFormLoading] = useState(false);
    const [deleteLoading, setDeleteLoading] = useState(false);
    const [openImport, setOpenImport] = useState(false);
    const [importYear, setImportYear] = useState(new Date().getFullYear());
    const [importLoading, setImportLoading] = useState(false);

    const fetchHolidays = async () => {
        setLoading(true);
        try {
            const res = await leaveApi.get('/holidays');
            setHolidays(res.data);
            setError('');
        } catch (err) {
            setError('Failed to load holidays');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchHolidays();
    }, []);

    const handleFormChange = (e) => {
        const { name, value, type, checked } = e.target;
        setForm(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
    };

    const handleAddHoliday = async (e) => {
        e.preventDefault();
        setFormLoading(true);
        try {
            await leaveApi.post('/holidays', form);
            setSnackbar({ open: true, message: 'Holiday added!', severity: 'success' });
            setOpenAdd(false);
            setForm({ name: '', date: '', description: '', isPublic: true });
            fetchHolidays();
        } catch (err) {
            setSnackbar({ open: true, message: 'Failed to add holiday', severity: 'error' });
        } finally {
            setFormLoading(false);
        }
    };

    const handleOpenEdit = (holiday) => {
        setSelectedHoliday(holiday);
        setForm({
            name: holiday.name,
            date: holiday.date,
            description: holiday.description,
            isPublic: holiday.isPublic
        });
        setOpenEdit(true);
    };

    const handleEditHoliday = async (e) => {
        e.preventDefault();
        setFormLoading(true);
        try {
            await leaveApi.put(`/holidays/${selectedHoliday.id}`, form);
            setSnackbar({ open: true, message: 'Holiday updated!', severity: 'success' });
            setOpenEdit(false);
            setSelectedHoliday(null);
            setForm({ name: '', date: '', description: '', isPublic: true });
            fetchHolidays();
        } catch (err) {
            setSnackbar({ open: true, message: 'Failed to update holiday', severity: 'error' });
        } finally {
            setFormLoading(false);
        }
    };

    const handleOpenDelete = (holiday) => {
        setSelectedHoliday(holiday);
        setOpenDelete(true);
    };

    const handleDeleteHoliday = async () => {
        if (!selectedHoliday) return;
        setDeleteLoading(true);
        try {
            await leaveApi.delete(`/holidays/${selectedHoliday.id}`);
            setSnackbar({ open: true, message: 'Holiday deleted!', severity: 'success' });
            setOpenDelete(false);
            setSelectedHoliday(null);
            fetchHolidays();
        } catch (err) {
            setSnackbar({ open: true, message: 'Failed to delete holiday', severity: 'error' });
        } finally {
            setDeleteLoading(false);
        }
    };

    return (
        <Box sx={{ p: 4 }}>
            <Typography variant="h4" sx={{ mb: 3 }}>Holidays Management</Typography>
            <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
                <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpenAdd(true)}>Add Holiday</Button>
                <Button variant="outlined" onClick={() => setOpenImport(true)}>Import Public Holidays</Button>
            </Box>
            {loading ? (
                <CircularProgress />
            ) : error ? (
                <Alert severity="error">{error}</Alert>
            ) : (
                <TableContainer component={Paper}>
                    <Table size="small">
                        <TableHead>
                            <TableRow>
                                <TableCell>Name</TableCell>
                                <TableCell>Date</TableCell>
                                <TableCell>Description</TableCell>
                                <TableCell>Public</TableCell>
                                <TableCell align="right">Actions</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {holidays.map(h => (
                                <TableRow key={h.id}>
                                    <TableCell>{h.name}</TableCell>
                                    <TableCell>{h.date}</TableCell>
                                    <TableCell>{h.description}</TableCell>
                                    <TableCell>{h.isPublic ? 'Yes' : 'No'}</TableCell>
                                    <TableCell align="right">
                                        <IconButton color="primary" onClick={() => handleOpenEdit(h)}><EditIcon /></IconButton>
                                        <IconButton color="error" onClick={() => handleOpenDelete(h)}><DeleteIcon /></IconButton>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            )}
            {/* Add Holiday Dialog */}
            <Dialog open={openAdd} onClose={() => setOpenAdd(false)} maxWidth="sm" fullWidth>
                <DialogTitle>Add Holiday</DialogTitle>
                <form onSubmit={handleAddHoliday}>
                    <DialogContent>
                        <TextField label="Name" name="name" value={form.name} onChange={handleFormChange} fullWidth margin="normal" required />
                        <TextField label="Date" name="date" type="date" value={form.date} onChange={handleFormChange} fullWidth margin="normal" InputLabelProps={{ shrink: true }} required />
                        <TextField label="Description" name="description" value={form.description} onChange={handleFormChange} fullWidth margin="normal" />
                        {/* You can add a checkbox for isPublic if needed */}
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={() => setOpenAdd(false)} disabled={formLoading}>Cancel</Button>
                        <Button type="submit" variant="contained" disabled={formLoading}>{formLoading ? <CircularProgress size={20} /> : 'Add'}</Button>
                    </DialogActions>
                </form>
            </Dialog>
            {/* Edit Holiday Dialog */}
            <Dialog open={openEdit} onClose={() => setOpenEdit(false)} maxWidth="sm" fullWidth>
                <DialogTitle>Edit Holiday</DialogTitle>
                <form onSubmit={handleEditHoliday}>
                    <DialogContent>
                        <TextField label="Name" name="name" value={form.name} onChange={handleFormChange} fullWidth margin="normal" required />
                        <TextField label="Date" name="date" type="date" value={form.date} onChange={handleFormChange} fullWidth margin="normal" InputLabelProps={{ shrink: true }} required />
                        <TextField label="Description" name="description" value={form.description} onChange={handleFormChange} fullWidth margin="normal" />
                        {/* You can add a checkbox for isPublic if needed */}
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={() => setOpenEdit(false)} disabled={formLoading}>Cancel</Button>
                        <Button type="submit" variant="contained" disabled={formLoading}>{formLoading ? <CircularProgress size={20} /> : 'Save'}</Button>
                    </DialogActions>
                </form>
            </Dialog>
            {/* Delete Holiday Dialog */}
            <Dialog open={openDelete} onClose={() => setOpenDelete(false)}>
                <DialogTitle>Delete Holiday</DialogTitle>
                <DialogContent>
                    <Typography>Are you sure you want to delete the holiday "{selectedHoliday?.name}"?</Typography>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setOpenDelete(false)} disabled={deleteLoading}>Cancel</Button>
                    <Button color="error" variant="contained" onClick={handleDeleteHoliday} disabled={deleteLoading}>{deleteLoading ? <CircularProgress size={20} /> : 'Delete'}</Button>
                </DialogActions>
            </Dialog>
            {/* Import Public Holidays Dialog */}
            <Dialog open={openImport} onClose={() => setOpenImport(false)} maxWidth="xs" fullWidth>
                <DialogTitle>Import Public Holidays</DialogTitle>
                <DialogContent>
                    <TextField
                        label="Year"
                        type="number"
                        value={importYear}
                        onChange={e => setImportYear(Number(e.target.value))}
                        fullWidth
                        margin="normal"
                        inputProps={{ min: 2000, max: 2100 }}
                    />
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                        This will import all public holidays for the selected year from the Nager API (country code as configured in backend).
                    </Typography>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setOpenImport(false)} disabled={importLoading}>Cancel</Button>
                    <Button
                        variant="contained"
                        onClick={async () => {
                            setImportLoading(true);
                            try {
                                await leaveApi.post(`/holidays/import-public-holidays?year=${importYear}`);
                                setSnackbar({ open: true, message: `Imported public holidays for ${importYear}!`, severity: 'success' });
                                setOpenImport(false);
                                fetchHolidays();
                            } catch (err) {
                                setSnackbar({ open: true, message: 'Failed to import public holidays', severity: 'error' });
                            } finally {
                                setImportLoading(false);
                            }
                        }}
                        disabled={importLoading}
                    >
                        {importLoading ? <CircularProgress size={20} /> : 'Import'}
                    </Button>
                </DialogActions>
            </Dialog>
            <Snackbar open={snackbar.open} autoHideDuration={3000} onClose={() => setSnackbar({ ...snackbar, open: false })}>
                <Alert severity={snackbar.severity} sx={{ width: '100%' }}>{snackbar.message}</Alert>
            </Snackbar>
        </Box>
    );
};

export default Holidays; 