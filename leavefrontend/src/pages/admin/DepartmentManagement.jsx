import React, { useEffect, useState } from 'react';
import { Box, Typography, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Button, Chip, CircularProgress, IconButton, Dialog, DialogTitle, DialogContent, DialogActions, TextField, FormControl, InputLabel, Select } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import GroupIcon from '@mui/icons-material/Group';
import ToggleOnIcon from '@mui/icons-material/ToggleOn';
import ToggleOffIcon from '@mui/icons-material/ToggleOff';
import { leaveApi, authApi } from '@/lib/api';
import Snackbar from '@mui/material/Snackbar';
import MenuItem from '@mui/material/MenuItem';

const DepartmentManagement = () => {
    const [departments, setDepartments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [openAdd, setOpenAdd] = useState(false);
    const [openEdit, setOpenEdit] = useState(false);
    const [openDelete, setOpenDelete] = useState(false);
    const [selectedDept, setSelectedDept] = useState(null);
    const [form, setForm] = useState({ name: '', description: '', managerId: '' });
    const [managers, setManagers] = useState([]);
    const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
    const [openMembers, setOpenMembers] = useState(false);
    const [membersDept, setMembersDept] = useState(null);
    const [allUsers, setAllUsers] = useState([]);
    const [assignUserId, setAssignUserId] = useState('');

    useEffect(() => {
        fetchDepartments();
        // Fetch managers for dropdown
        authApi.get('/users').then(res => {
            setManagers(res.data.filter(u => u.role === 'MANAGER'));
        });
        // Fetch all users for assignment
        authApi.get('/users').then(res => {
            setAllUsers(res.data);
        });
    }, []);

    const fetchDepartments = async () => {
        setLoading(true);
        try {
            const res = await authApi.get('/departments');
            setDepartments(res.data);
        } catch (err) {
            setError('Failed to load departments');
        } finally {
            setLoading(false);
        }
    };

    const handleOpenAdd = () => {
        setForm({ name: '', description: '', managerId: '' });
        setOpenAdd(true);
    };
    const handleOpenEdit = (dept) => {
        setSelectedDept(dept);
        setForm({
            name: dept.name,
            description: dept.description,
            managerId: dept.manager ? dept.manager.id : ''
        });
        setOpenEdit(true);
    };
    const handleOpenDelete = (dept) => {
        setSelectedDept(dept);
        setOpenDelete(true);
    };
    const handleCloseDialogs = () => {
        setOpenAdd(false);
        setOpenEdit(false);
        setOpenDelete(false);
        setSelectedDept(null);
    };
    const handleFormChange = (e) => {
        const { name, value } = e.target;
        setForm(prev => ({ ...prev, [name]: value }));
    };
    const handleAddDepartment = async (e) => {
        e.preventDefault();
        try {
            await authApi.post('/departments', {
                name: form.name,
                description: form.description,
                managerId: form.managerId || undefined
            });
            setSnackbar({ open: true, message: 'Department added!', severity: 'success' });
            setOpenAdd(false);
            fetchDepartments();
        } catch (err) {
            setSnackbar({ open: true, message: err.response?.data || 'Failed to add department', severity: 'error' });
        }
    };
    const handleEditDepartment = async (e) => {
        e.preventDefault();
        try {
            await authApi.put(`/departments/${selectedDept.id}`, {
                name: form.name,
                description: form.description,
                managerId: form.managerId || undefined
            });
            setSnackbar({ open: true, message: 'Department updated!', severity: 'success' });
            setOpenEdit(false);
            setSelectedDept(null);
            fetchDepartments();
        } catch (err) {
            setSnackbar({ open: true, message: err.response?.data || 'Failed to update department', severity: 'error' });
        }
    };
    const handleDeleteDepartment = async () => {
        if (!selectedDept) return;
        try {
            await authApi.delete(`/departments/${selectedDept.id}`);
            setSnackbar({ open: true, message: 'Department deleted!', severity: 'success' });
            setOpenDelete(false);
            setSelectedDept(null);
            fetchDepartments();
        } catch (err) {
            setSnackbar({ open: true, message: err.response?.data || 'Failed to delete department', severity: 'error' });
        }
    };
    const handleToggleActive = async (dept) => {
        const url = `/departments/${dept.id}/${dept.isActive ? 'deactivate' : 'activate'}`;
        try {
            await authApi.put(url);
            setDepartments(departments =>
                departments.map(d =>
                    d.id === dept.id ? { ...d, isActive: !dept.isActive } : d
                )
            );
            setSnackbar({ open: true, message: `Department ${dept.isActive ? 'deactivated' : 'activated'}!`, severity: 'success' });
        } catch (err) {
            setSnackbar({ open: true, message: err.response?.data || 'Failed to update department status', severity: 'error' });
        }
    };
    const handleOpenMembers = (dept) => {
        setMembersDept(dept);
        setOpenMembers(true);
    };
    const handleCloseMembers = () => {
        setOpenMembers(false);
        setMembersDept(null);
        setAssignUserId('');
    };
    const handleAssignUser = async () => {
        if (!assignUserId || !membersDept) return;
        try {
            await authApi.put(`/departments/assign-user?userId=${assignUserId}&departmentId=${membersDept.id}`);
            setSnackbar({ open: true, message: 'User assigned to department!', severity: 'success' });
            // Refresh departments and dialog
            const res = await authApi.get('/departments');
            setDepartments(res.data);
            const updatedDept = res.data.find(d => d.id === membersDept.id);
            setMembersDept(updatedDept);
            setAssignUserId('');
        } catch (err) {
            setSnackbar({ open: true, message: err.response?.data || 'Failed to assign user', severity: 'error' });
        }
    };
    const handleRemoveUser = async (userId) => {
        if (!membersDept) return;
        try {
            await authApi.put(`/departments/remove-user?userId=${userId}`);
            setSnackbar({ open: true, message: 'User removed from department!', severity: 'success' });
            // Refresh departments and dialog
            const res = await authApi.get('/departments');
            setDepartments(res.data);
            const updatedDept = res.data.find(d => d.id === membersDept.id);
            setMembersDept(updatedDept);
        } catch (err) {
            setSnackbar({ open: true, message: err.response?.data || 'Failed to remove user', severity: 'error' });
        }
    };

    return (
        <Box sx={{ p: 4 }}>
            <Typography variant="h4" sx={{ mb: 3 }}>
                Department Management
            </Typography>
            <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
                <Button variant="contained" startIcon={<AddIcon />} onClick={handleOpenAdd}>Add Department</Button>
            </Box>
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
                                <TableCell>Manager</TableCell>
                                <TableCell># Members</TableCell>
                                <TableCell>Status</TableCell>
                                <TableCell align="right">Actions</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {departments.map((dept) => (
                                <TableRow key={dept.id}>
                                    <TableCell>{dept.name}</TableCell>
                                    <TableCell>{dept.description}</TableCell>
                                    <TableCell>{dept.managerName ? dept.managerName : '-'}</TableCell>
                                    <TableCell>{dept.users ? dept.users.length : 0}</TableCell>
                                    <TableCell>
                                        {dept.isActive ? <Chip label="Active" color="success" /> : <Chip label="Inactive" color="default" />}
                                    </TableCell>
                                    <TableCell align="right">
                                        <IconButton color="primary" onClick={() => handleOpenEdit(dept)}><EditIcon /></IconButton>
                                        <IconButton color="error" onClick={() => handleOpenDelete(dept)}><DeleteIcon /></IconButton>
                                        <IconButton color="info" onClick={() => handleOpenMembers(dept)}><GroupIcon /></IconButton>
                                        <IconButton color={dept.isActive ? 'success' : 'default'} onClick={() => handleToggleActive(dept)}>
                                            {dept.isActive ? <ToggleOnIcon /> : <ToggleOffIcon />}
                                        </IconButton>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            )}
            {/* Add Department Dialog */}
            <Dialog open={openAdd} onClose={handleCloseDialogs} maxWidth="sm" fullWidth>
                <DialogTitle>Add Department</DialogTitle>
                <form onSubmit={handleAddDepartment}>
                    <DialogContent>
                        <TextField label="Name" name="name" value={form.name} onChange={handleFormChange} fullWidth margin="normal" required />
                        <TextField label="Description" name="description" value={form.description} onChange={handleFormChange} fullWidth margin="normal" />
                        <FormControl fullWidth margin="normal">
                            <InputLabel>Manager</InputLabel>
                            <Select name="managerId" value={form.managerId} label="Manager" onChange={handleFormChange}>
                                <MenuItem value="">None</MenuItem>
                                {managers.map(m => (
                                    <MenuItem key={m.id} value={m.id}>{m.firstName} {m.lastName}</MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={handleCloseDialogs}>Cancel</Button>
                        <Button type="submit" variant="contained">Add</Button>
                    </DialogActions>
                </form>
            </Dialog>
            {/* Edit Department Dialog */}
            <Dialog open={openEdit} onClose={handleCloseDialogs} maxWidth="sm" fullWidth>
                <DialogTitle>Edit Department</DialogTitle>
                <form onSubmit={handleEditDepartment}>
                    <DialogContent>
                        <TextField label="Name" name="name" value={form.name} onChange={handleFormChange} fullWidth margin="normal" required />
                        <TextField label="Description" name="description" value={form.description} onChange={handleFormChange} fullWidth margin="normal" />
                        <FormControl fullWidth margin="normal">
                            <InputLabel>Manager</InputLabel>
                            <Select name="managerId" value={form.managerId} label="Manager" onChange={handleFormChange}>
                                <MenuItem value="">None</MenuItem>
                                {managers.map(m => (
                                    <MenuItem key={m.id} value={m.id}>{m.firstName} {m.lastName}</MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={handleCloseDialogs}>Cancel</Button>
                        <Button type="submit" variant="contained">Save</Button>
                    </DialogActions>
                </form>
            </Dialog>
            {/* Delete Department Dialog */}
            <Dialog open={openDelete} onClose={handleCloseDialogs}>
                <DialogTitle>Delete Department</DialogTitle>
                <DialogContent>
                    <Typography>Are you sure you want to delete the department "{selectedDept?.name}"?</Typography>
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleCloseDialogs}>Cancel</Button>
                    <Button color="error" variant="contained" onClick={handleDeleteDepartment}>Delete</Button>
                </DialogActions>
            </Dialog>
            {/* Department Members Dialog */}
            <Dialog open={openMembers} onClose={handleCloseMembers} maxWidth="sm" fullWidth>
                <DialogTitle>Department Members - {membersDept?.name}</DialogTitle>
                <DialogContent>
                    <Typography variant="subtitle1" sx={{ mb: 1 }}>Current Members:</Typography>
                    {membersDept?.users && membersDept.users.length > 0 ? (
                        <ul>
                            {membersDept.users.map(u => (
                                <li key={u.id}>
                                    {u.firstName} {u.lastName} ({u.email})
                                    <Button size="small" color="error" sx={{ ml: 1 }} onClick={() => handleRemoveUser(u.id)}>Remove</Button>
                                </li>
                            ))}
                        </ul>
                    ) : (
                        <Typography color="text.secondary">No members in this department.</Typography>
                    )}
                    <Box sx={{ mt: 2 }}>
                        <Typography variant="subtitle2">Assign User:</Typography>
                        <FormControl fullWidth>
                            <InputLabel>User</InputLabel>
                            <Select value={assignUserId} label="User" onChange={e => setAssignUserId(e.target.value)}>
                                <MenuItem value="">Select user</MenuItem>
                                {allUsers.filter(u => (u.role === 'STAFF' || u.role === 'MANAGER') && !membersDept?.users?.some(m => m.id === u.id)).map(u => (
                                    <MenuItem key={u.id} value={u.id}>{u.firstName} {u.lastName} ({u.email})</MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                        <Button sx={{ mt: 1 }} variant="contained" disabled={!assignUserId} onClick={handleAssignUser}>Assign</Button>
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleCloseMembers}>Close</Button>
                </DialogActions>
            </Dialog>
            <Snackbar open={snackbar.open} autoHideDuration={3000} onClose={() => setSnackbar({ ...snackbar, open: false })} message={snackbar.message} />
        </Box>
    );
};

export default DepartmentManagement; 