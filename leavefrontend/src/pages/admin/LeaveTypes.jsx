import React, { useEffect, useState } from 'react';
import { Box, Typography, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Button, Chip, CircularProgress, IconButton, Dialog, DialogTitle, DialogContent, DialogActions, TextField, FormControlLabel, Checkbox, Switch, Alert, Snackbar } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import ToggleOnIcon from '@mui/icons-material/ToggleOn';
import ToggleOffIcon from '@mui/icons-material/ToggleOff';
import LockIcon from '@mui/icons-material/Lock';
import Tooltip from '@mui/material/Tooltip';
import { leaveApi } from '@/lib/api';
import dayjs from 'dayjs';

const SYSTEM_LEAVE_TYPES = [
    'Annual Leave',
    'Sick Leave',
    'Maternity Leave',
    'Compassionate Leave',
];

const LeaveTypes = () => {
    const [leaveTypes, setLeaveTypes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [openAdd, setOpenAdd] = useState(false);
    const [openEdit, setOpenEdit] = useState(false);
    const [openDelete, setOpenDelete] = useState(false);
    const [selectedType, setSelectedType] = useState(null);
    const [form, setForm] = useState({
        name: '',
        description: '',
        defaultDays: '',
        accrualRate: '',
        isActive: true,
        canCarryOver: false,
        maxCarryOverDays: '',
        requiresApproval: true,
        requiresDocumentation: false,
        isPaid: true
    });
    const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

    const fetchLeaveTypes = async () => {
        setLoading(true);
        try {
            const res = await leaveApi.get('/leave-types');
            setLeaveTypes(res.data);
            setError('');
        } catch (err) {
            setError('Failed to load leave types');
            console.error('Fetch error:', err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchLeaveTypes();
    }, []);

    const handleOpenAdd = () => {
        setOpenAdd(true);
    };

    const handleCloseAdd = () => {
        setOpenAdd(false);
    };

    const handleFormChange = (e) => {
        const { name, value, type, checked } = e.target;
        setForm({
            ...form,
            [name]: type === 'checkbox' ? checked : value
        });
    };

    const handleAddLeaveType = async (e) => {
        e.preventDefault();
        try {
            await leaveApi.post('/leave-types', {
                name: form.name,
                description: form.description,
                defaultDays: Number(form.defaultDays),
                accrualRate: Number(form.accrualRate),
                isActive: form.isActive,
                canCarryOver: form.canCarryOver,
                maxCarryOverDays: form.canCarryOver ? Number(form.maxCarryOverDays) : 0,
                requiresApproval: form.requiresApproval,
                requiresDocumentation: form.requiresDocumentation,
                isPaid: form.isPaid
            });
            setSnackbar({ open: true, message: 'Leave type added!', severity: 'success' });
            setOpenAdd(false);
            fetchLeaveTypes();
        } catch (err) {
            setSnackbar({ open: true, message: err.response?.data || 'Failed to add leave type', severity: 'error' });
        }
    };

    const handleOpenEdit = (type) => {
        setSelectedType(type);
        setForm({
            name: type.name,
            description: type.description,
            defaultDays: type.defaultDays,
            accrualRate: type.accrualRate,
            isActive: type.isActive,
            canCarryOver: type.canCarryOver,
            maxCarryOverDays: type.maxCarryOverDays,
            requiresApproval: type.requiresApproval,
            requiresDocumentation: type.requiresDocumentation,
            isPaid: type.isPaid
        });
        setOpenEdit(true);
    };

    const handleCloseEdit = () => {
        setOpenEdit(false);
        setSelectedType(null);
    };

    const handleEditLeaveType = async (e) => {
        e.preventDefault();
        try {
            await leaveApi.put(`/leave-types/${selectedType.id}`, {
                name: form.name,
                description: form.description,
                defaultDays: Number(form.defaultDays),
                accrualRate: Number(form.accrualRate),
                isActive: form.isActive,
                canCarryOver: form.canCarryOver,
                maxCarryOverDays: form.canCarryOver ? Number(form.maxCarryOverDays) : 0,
                requiresApproval: form.requiresApproval,
                requiresDocumentation: form.requiresDocumentation,
                isPaid: form.isPaid
            });
            setSnackbar({ open: true, message: 'Leave type updated!', severity: 'success' });
            setOpenEdit(false);
            setSelectedType(null);
            fetchLeaveTypes();
        } catch (err) {
            setSnackbar({ open: true, message: err.response?.data || 'Failed to update leave type', severity: 'error' });
        }
    };

    const handleOpenDelete = (type) => {
        setSelectedType(type);
        setOpenDelete(true);
    };

    const handleCloseDelete = () => {
        setOpenDelete(false);
    };

    const handleDeleteLeaveType = async () => {
        if (!selectedType) return;
        try {
            await leaveApi.delete(`/leave-types/${selectedType.id}`);
            setSnackbar({ open: true, message: 'Leave type deleted!', severity: 'success' });
            setOpenDelete(false);
            setSelectedType(null);
            fetchLeaveTypes();
        } catch (err) {
            setSnackbar({ open: true, message: err.response?.data || 'Failed to delete leave type', severity: 'error' });
        }
    };

    const handleToggleActive = async (type) => {
        try {
            await leaveApi.put(`/leave-types/${type.id}`, {
                ...type,
                isActive: !type.isActive
            });
            setSnackbar({ open: true, message: 'Leave type status updated!', severity: 'success' });
            await fetchLeaveTypes();
        } catch (err) {
            console.error('Toggle error:', err);
            setSnackbar({ open: true, message: err.response?.data || 'Failed to update leave type status', severity: 'error' });
        }
    };

    return (
        <Box sx={{ p: 4 }}>
            <Typography variant="h4" sx={{ mb: 3 }}>
                Leave Types Management
            </Typography>
            <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
                <Button variant="contained" startIcon={<AddIcon />} onClick={handleOpenAdd}>Add Leave Type</Button>
            </Box>
            {/* Add Leave Type Dialog - always rendered at top level */}
            <Dialog open={openAdd} onClose={handleCloseAdd} maxWidth="sm" fullWidth>
                <DialogTitle>Add Leave Type</DialogTitle>
                <form onSubmit={handleAddLeaveType}>
                    <DialogContent>
                        <TextField label="Name" name="name" value={form.name} onChange={handleFormChange} fullWidth margin="normal" required />
                        <TextField label="Description" name="description" value={form.description} onChange={handleFormChange} fullWidth margin="normal" />
                        <TextField label="Default Days" name="defaultDays" value={form.defaultDays} onChange={handleFormChange} type="number" fullWidth margin="normal" required />
                        <TextField label="Accrual Rate" name="accrualRate" value={form.accrualRate} onChange={handleFormChange} type="number" fullWidth margin="normal" required />
                        <FormControlLabel control={<Checkbox checked={form.isActive} onChange={handleFormChange} name="isActive" />} label="Active" />
                        <FormControlLabel control={<Checkbox checked={form.canCarryOver} onChange={handleFormChange} name="canCarryOver" />} label="Carryover Allowed" />
                        <TextField label="Carryover Cap" name="maxCarryOverDays" value={form.maxCarryOverDays} onChange={handleFormChange} type="number" fullWidth margin="normal" disabled={!form.canCarryOver} />
                        <FormControlLabel control={<Checkbox checked={form.requiresApproval} onChange={handleFormChange} name="requiresApproval" />} label="Requires Approval" />
                        <FormControlLabel control={<Checkbox checked={form.requiresDocumentation} onChange={handleFormChange} name="requiresDocumentation" />} label="Requires Documentation" />
                        <FormControlLabel control={<Checkbox checked={form.isPaid} onChange={handleFormChange} name="isPaid" />} label="Is Paid" />
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={handleCloseAdd}>Cancel</Button>
                        <Button type="submit" variant="contained">Add</Button>
                    </DialogActions>
                </form>
            </Dialog>
            {/* Edit Leave Type Dialog */}
            <Dialog open={openEdit} onClose={handleCloseEdit} maxWidth="sm" fullWidth>
                <DialogTitle>Edit Leave Type</DialogTitle>
                <form onSubmit={handleEditLeaveType}>
                    <DialogContent>
                        <TextField label="Name" name="name" value={form.name} onChange={handleFormChange} fullWidth margin="normal" required />
                        <TextField label="Description" name="description" value={form.description} onChange={handleFormChange} fullWidth margin="normal" />
                        <TextField label="Default Days" name="defaultDays" value={form.defaultDays} onChange={handleFormChange} type="number" fullWidth margin="normal" required />
                        <TextField label="Accrual Rate" name="accrualRate" value={form.accrualRate} onChange={handleFormChange} type="number" fullWidth margin="normal" required />
                        <FormControlLabel control={<Checkbox checked={form.isActive} onChange={handleFormChange} name="isActive" />} label="Active" />
                        <FormControlLabel control={<Checkbox checked={form.canCarryOver} onChange={handleFormChange} name="canCarryOver" />} label="Carryover Allowed" />
                        <TextField label="Carryover Cap" name="maxCarryOverDays" value={form.maxCarryOverDays} onChange={handleFormChange} type="number" fullWidth margin="normal" disabled={!form.canCarryOver} />
                        <FormControlLabel control={<Checkbox checked={form.requiresApproval} onChange={handleFormChange} name="requiresApproval" />} label="Requires Approval" />
                        <FormControlLabel control={<Checkbox checked={form.requiresDocumentation} onChange={handleFormChange} name="requiresDocumentation" />} label="Requires Documentation" />
                        <FormControlLabel control={<Checkbox checked={form.isPaid} onChange={handleFormChange} name="isPaid" />} label="Is Paid" />
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={handleCloseEdit}>Cancel</Button>
                        <Button type="submit" variant="contained">Save</Button>
                    </DialogActions>
                </form>
            </Dialog>
            {/* Delete Leave Type Dialog */}
            <Dialog open={openDelete} onClose={handleCloseDelete}>
                <DialogTitle>Delete Leave Type</DialogTitle>
                <DialogContent>
                    <Typography>Are you sure you want to delete the leave type "{selectedType?.name}"?</Typography>
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleCloseDelete}>Cancel</Button>
                    <Button color="error" variant="contained" onClick={handleDeleteLeaveType}>Delete</Button>
                </DialogActions>
            </Dialog>
            {loading ? (
                <Box display="flex" justifyContent="center" alignItems="center" minHeight="200px">
                    <CircularProgress />
                </Box>
            ) : error ? (
                <Typography color="error">{error}</Typography>
            ) : (
                <TableContainer component={Paper}>
                    <Table>
                        <TableHead>
                            <TableRow>
                                <TableCell>Name</TableCell>
                                <TableCell>Description</TableCell>
                                <TableCell>Default Days</TableCell>
                                <TableCell>Accrual Rate</TableCell>
                                <TableCell>Carryover Allowed</TableCell>
                                <TableCell>Carryover Cap</TableCell>
                                <TableCell>Status</TableCell>
                                <TableCell align="right">Actions</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {leaveTypes.map((type) => {
                                const isSystemType = SYSTEM_LEAVE_TYPES.includes(type.name);
                                return (
                                    <TableRow key={type.id}>
                                        <TableCell>{type.name} {isSystemType && (
                                            <Tooltip title="This leave type is required by law and cannot be edited or deleted.">
                                                <LockIcon fontSize="small" color="action" style={{ verticalAlign: 'middle', marginLeft: 4 }} />
                                            </Tooltip>
                                        )}</TableCell>
                                        <TableCell>{type.description}</TableCell>
                                        <TableCell>{type.defaultDays}</TableCell>
                                        <TableCell>{type.accrualRate}</TableCell>
                                        <TableCell>{type.canCarryOver ? 'Yes' : 'No'}</TableCell>
                                        <TableCell>{type.maxCarryOverDays}</TableCell>
                                        <TableCell>
                                            {type.isActive ? <Chip label="Active" color="success" /> : <Chip label="Inactive" color="default" />}
                                        </TableCell>
                                        <TableCell align="right">
                                            <Tooltip title={isSystemType ? "Editing is disabled for system leave types." : "Edit"}>
                                                <span>
                                                    <IconButton color="primary" onClick={() => handleOpenEdit(type)} disabled={isSystemType}>
                                                        <EditIcon />
                                                    </IconButton>
                                                </span>
                                            </Tooltip>
                                            <Tooltip title={isSystemType ? "Deleting is disabled for system leave types." : "Delete"}>
                                                <span>
                                                    <IconButton color="error" onClick={() => handleOpenDelete(type)} disabled={isSystemType}>
                                                        <DeleteIcon />
                                                    </IconButton>
                                                </span>
                                            </Tooltip>
                                            <IconButton color={type.isActive ? 'success' : 'default'} onClick={() => handleToggleActive(type)}>
                                                {type.isActive ? <ToggleOnIcon /> : <ToggleOffIcon />}
                                            </IconButton>
                                        </TableCell>
                                    </TableRow>
                                );
                            })}
                        </TableBody>
                    </Table>
                </TableContainer>
            )}
            <Snackbar open={snackbar.open} autoHideDuration={3000} onClose={() => setSnackbar({ ...snackbar, open: false })} message={snackbar.message} />
        </Box>
    );
}

export default LeaveTypes; 