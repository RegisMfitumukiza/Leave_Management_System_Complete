import React, { useEffect, useState } from 'react';
import { Box, Typography, Paper, Button, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, IconButton, Chip, CircularProgress, Dialog, DialogTitle, DialogContent, DialogActions, Alert, TextField, MenuItem, FormControl, InputLabel, Select, Snackbar, Tabs, Tab } from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import { authApi, leaveApi } from '@/lib/api';
import dayjs from 'dayjs';

const UserManagement = () => {
    const [users, setUsers] = useState([]);
    const [departments, setDepartments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [openAddDialog, setOpenAddDialog] = useState(false);
    const [search, setSearch] = useState('');
    const [roleFilter, setRoleFilter] = useState('');
    const [deptFilter, setDeptFilter] = useState('');
    const [userForm, setUserForm] = useState({
        firstName: '',
        lastName: '',
        email: '',
        password: '',
        role: 'STAFF',
        departmentId: ''
    });
    const [userLoading, setUserLoading] = useState(false);
    const [userError, setUserError] = useState('');
    const [userSuccess, setUserSuccess] = useState(false);
    const [editUser, setEditUser] = useState(null);
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeactivateDialog, setShowDeactivateDialog] = useState(false);
    const [deactivateUser, setDeactivateUser] = useState(null);
    const [showActivateDialog, setShowActivateDialog] = useState(false);
    const [activateUser, setActivateUser] = useState(null);
    const [adjustUser, setAdjustUser] = useState(null);
    const [showAdjustDialog, setShowAdjustDialog] = useState(false);
    const [viewUser, setViewUser] = useState(null);
    const [showViewDialog, setShowViewDialog] = useState(false);
    const [selectedUsers, setSelectedUsers] = useState([]);
    const [showBulkDeactivate, setShowBulkDeactivate] = useState(false);
    const [showBulkAssignDept, setShowBulkAssignDept] = useState(false);
    const [editForm, setEditForm] = useState({ firstName: '', lastName: '', email: '', role: '', departmentId: '' });
    const [editLoading, setEditLoading] = useState(false);
    const [editError, setEditError] = useState('');
    const [deactivateLoading, setDeactivateLoading] = useState(false);
    const [deactivateError, setDeactivateError] = useState('');
    const [activateLoading, setActivateLoading] = useState(false);
    const [activateError, setActivateError] = useState('');
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [deleteUser, setDeleteUser] = useState(null);
    const [showBulkActivate, setShowBulkActivate] = useState(false);
    const [leaveTypes, setLeaveTypes] = useState([]);
    const [userBalances, setUserBalances] = useState([]);
    const [adjustForm, setAdjustForm] = useState({ leaveTypeId: '', adjustmentDays: '', reason: '' });
    const [adjustLoading, setAdjustLoading] = useState(false);
    const [adjustError, setAdjustError] = useState('');
    const [adjustSuccess, setAdjustSuccess] = useState(false);
    const [userDetails, setUserDetails] = useState(null);
    const [userDetailsBalances, setUserDetailsBalances] = useState([]);
    const [userDetailsHistory, setUserDetailsHistory] = useState([]);
    const [userDetailsTab, setUserDetailsTab] = useState(0);
    const [userDetailsLoading, setUserDetailsLoading] = useState(false);
    const [userDetailsError, setUserDetailsError] = useState('');
    const [bulkAssignDeptId, setBulkAssignDeptId] = useState('');
    const [bulkAssignLoading, setBulkAssignLoading] = useState(false);
    const [bulkAssignError, setBulkAssignError] = useState('');
    const [bulkAssignSuccess, setBulkAssignSuccess] = useState(false);
    const [showBulkAdjust, setShowBulkAdjust] = useState(false);
    const [bulkAdjustLeaveTypeId, setBulkAdjustLeaveTypeId] = useState('');
    const [bulkAdjustAmount, setBulkAdjustAmount] = useState('');
    const [bulkAdjustReason, setBulkAdjustReason] = useState('');
    const [bulkAdjustLoading, setBulkAdjustLoading] = useState(false);
    const [bulkAdjustError, setBulkAdjustError] = useState('');
    const [bulkAdjustSuccess, setBulkAdjustSuccess] = useState(false);

    useEffect(() => {
        fetchUsers();
        fetchDepartments();
    }, []);

    const fetchUsers = async () => {
        setLoading(true);
        try {
            const res = await authApi.get('/users');
            setUsers(res.data);
        } catch (err) {
            setError('Failed to load users');
        } finally {
            setLoading(false);
        }
    };

    const fetchDepartments = async () => {
        try {
            const res = await authApi.get('/departments');
            setDepartments(res.data);
        } catch (err) {
            console.error('Failed to load departments:', err);
        }
    };

    useEffect(() => {
        if (editUser) {
            setEditForm({
                firstName: editUser.firstName || '',
                lastName: editUser.lastName || '',
                email: editUser.email || '',
                role: editUser.role || '',
                departmentId: editUser.department ? editUser.department.id : ''
            });
        }
    }, [editUser]);

    const handleOpenAddDialog = () => setOpenAddDialog(true);
    const handleCloseAddDialog = () => setOpenAddDialog(false);

    const handleUserFormChange = (e) => {
        const { name, value } = e.target;
        setUserForm((prev) => ({ ...prev, [name]: value }));
    };

    const handleAddUser = async (e) => {
        e.preventDefault();
        setUserLoading(true);
        setUserError('');
        try {
            const payload = {
                firstName: userForm.firstName,
                lastName: userForm.lastName,
                email: userForm.email,
                username: userForm.email,
                password: userForm.password,
                role: userForm.role,
                department: userForm.departmentId ? { id: userForm.departmentId } : undefined
            };
            await authApi.post('/register', payload);
            setUserSuccess(true);
            setOpenAddDialog(false);
            setUserForm({ firstName: '', lastName: '', email: '', password: '', role: 'STAFF', departmentId: '' });
            fetchUsers();
        } catch (err) {
            setUserError(err.response?.data || 'Failed to add user');
        } finally {
            setUserLoading(false);
        }
    };

    // Filtering logic
    const filteredUsers = users.filter(user => {
        const matchesSearch =
            user.firstName.toLowerCase().includes(search.toLowerCase()) ||
            user.lastName.toLowerCase().includes(search.toLowerCase()) ||
            user.email.toLowerCase().includes(search.toLowerCase());
        const matchesRole = roleFilter ? user.role === roleFilter : true;
        const matchesDept = deptFilter ? (user.department && user.department.id === deptFilter) : true;
        return matchesSearch && matchesRole && matchesDept;
    });

    const handleEditFormChange = (e) => {
        const { name, value } = e.target;
        setEditForm((prev) => ({ ...prev, [name]: value }));
    };

    const handleEditUser = async (e) => {
        e.preventDefault();
        setEditLoading(true);
        setEditError('');
        try {
            // Update name/email
            await authApi.put(`/users/${editUser.id}`, {
                firstName: editForm.firstName,
                lastName: editForm.lastName,
                email: editForm.email
            });
            // Update role if changed
            if (editForm.role && editForm.role !== editUser.role) {
                await authApi.put(`/users/${editUser.id}/role`, { role: editForm.role });
            }
            // Update department if changed
            if (editForm.departmentId && (!editUser.department || editForm.departmentId !== editUser.department.id)) {
                await authApi.put(`/departments/assign-user?userId=${editUser.id}&departmentId=${editForm.departmentId}`);
            }
            // Refresh users
            const res = await authApi.get('/users');
            setUsers(res.data);
            setShowEditDialog(false);
            setEditUser(null);
        } catch (err) {
            setEditError(err.response?.data || 'Failed to update user');
        } finally {
            setEditLoading(false);
        }
    };

    const handleDeactivateUser = async () => {
        if (!deactivateUser) return;
        setDeactivateLoading(true);
        setDeactivateError('');
        try {
            await authApi.delete(`/users/${deactivateUser.id}`);
            // Optimistically update the user in the local state
            setUsers(users =>
                users.map(u =>
                    u.id === deactivateUser.id ? { ...u, isActive: false } : u
                )
            );
            setShowDeactivateDialog(false);
            setDeactivateUser(null);
        } catch (err) {
            setDeactivateError(err.response?.data || 'Failed to deactivate user');
        } finally {
            setDeactivateLoading(false);
        }
    };

    const handleActivateUser = async () => {
        if (!activateUser) return;
        setActivateLoading(true);
        setActivateError('');
        try {
            await authApi.put(`/users/${activateUser.id}/activate`);
            // Optimistically update the user in the local state
            setUsers(users =>
                users.map(u =>
                    u.id === activateUser.id ? { ...u, isActive: true } : u
                )
            );
            setShowActivateDialog(false);
            setActivateUser(null);
        } catch (err) {
            setActivateError(err.response?.data || 'Failed to activate user');
        } finally {
            setActivateLoading(false);
        }
    };

    const handleDeleteUser = async () => {
        if (!deleteUser) return;
        setDeactivateLoading(true);
        setDeactivateError("");
        try {
            await authApi.delete(`/users/${deleteUser.id}/hard`);
            const res = await authApi.get('/users');
            setUsers(res.data);
            setShowDeleteDialog(false);
            setDeleteUser(null);
        } catch (err) {
            setDeactivateError(err.response?.data || 'Failed to delete user');
        } finally {
            setDeactivateLoading(false);
        }
    };

    const handleBulkDeactivate = async () => {
        if (selectedUsers.length === 0) return;
        setDeactivateLoading(true);
        setDeactivateError('');
        try {
            await authApi.post('/users/bulk-deactivate', selectedUsers);
            // Optimistically update local state
            setUsers(users =>
                users.map(u =>
                    selectedUsers.includes(u.id) ? { ...u, isActive: false } : u
                )
            );
            setShowBulkDeactivate(false);
            setSelectedUsers([]);
        } catch (err) {
            setDeactivateError(err.response?.data || 'Failed to bulk deactivate users');
        } finally {
            setDeactivateLoading(false);
        }
    };

    const handleBulkActivate = async () => {
        if (selectedUsers.length === 0) return;
        setActivateLoading(true);
        setActivateError('');
        try {
            await Promise.all(selectedUsers.map(id => authApi.put(`/users/${id}/activate`)));
            // Optimistically update local state
            setUsers(users =>
                users.map(u =>
                    selectedUsers.includes(u.id) ? { ...u, isActive: true } : u
                )
            );
            setShowBulkActivate(false);
            setSelectedUsers([]);
        } catch (err) {
            setActivateError(err.response?.data || 'Failed to bulk activate users');
        } finally {
            setActivateLoading(false);
        }
    };

    // Fetch leave types and balances when opening adjust dialog
    const handleOpenAdjustDialog = (user) => {
        setAdjustUser(user);
        setShowAdjustDialog(true);
        setAdjustForm({ leaveTypeId: '', adjustmentDays: '', reason: '' });
        setAdjustError('');
        setAdjustSuccess(false);
        // Fetch leave types
        leaveApi.get('/leave-types').then(res => setLeaveTypes(res.data)).catch(() => setLeaveTypes([]));
        // Fetch user balances
        leaveApi.get(`/leave-balances/user/${user.id}`).then(res => setUserBalances(res.data)).catch(() => setUserBalances([]));
    };

    const handleAdjustFormChange = (e) => {
        const { name, value } = e.target;
        setAdjustForm(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmitAdjust = async (e) => {
        e.preventDefault();
        setAdjustLoading(true);
        setAdjustError('');
        setAdjustSuccess(false);
        try {
            await leaveApi.post('/leave-balances/adjust', {
                userId: adjustUser.id,
                leaveTypeId: adjustForm.leaveTypeId,
                adjustmentDays: Number(adjustForm.adjustmentDays),
                reason: adjustForm.reason
            });
            setAdjustSuccess(true);
            // Refresh balances
            const res = await leaveApi.get(`/leave-balances/user/${adjustUser.id}`);
            setUserBalances(res.data);
            setAdjustForm({ leaveTypeId: '', adjustmentDays: '', reason: '' });
        } catch (err) {
            setAdjustError(err.response?.data || 'Failed to adjust leave balance');
        } finally {
            setAdjustLoading(false);
        }
    };

    const handleOpenViewDialog = async (user) => {
        setViewUser(user);
        setShowViewDialog(true);
        setUserDetailsTab(0);
        setUserDetailsLoading(true);
        setUserDetailsError('');
        try {
            // Fetch user info (already have basic info)
            setUserDetails(user);
            // Fetch balances
            const balancesRes = await leaveApi.get(`/leave-balances/user/${user.id}`);
            setUserDetailsBalances(balancesRes.data);
            // Fetch leave history
            const historyRes = await leaveApi.get(`/leaves?userId=${user.id}`);
            setUserDetailsHistory(historyRes.data);
        } catch (err) {
            setUserDetailsError('Failed to load user details');
        } finally {
            setUserDetailsLoading(false);
        }
    };

    const handleFixDepartments = async () => {
        try {
            const response = await authApi.post('/users/fix-departments');
            // Show success message
            alert(`Successfully fixed ${response.data.fixedCount} users without department assignment`);
            // Refresh the users list to show updated department assignments
            fetchUsers();
        } catch (error) {
            alert(error.response?.data?.message || 'Failed to fix department assignments');
        }
    };

    const handleSyncProfilePictures = async () => {
        try {
            const response = await authApi.post('/users/sync-profile-pictures');
            // Show success message
            alert(`Found ${response.data.syncedCount} users with Google IDs but no avatars`);
            // Refresh the users list to show updated profile pictures
            fetchUsers();
        } catch (error) {
            alert(error.response?.data?.message || 'Failed to sync profile pictures');
        }
    };

    return (
        <Box sx={{ p: 4 }}>
            <Typography variant="h4" sx={{ mb: 3 }}>
                User Management
            </Typography>
            <Box sx={{ display: 'flex', gap: 2, mb: 2, flexWrap: 'wrap' }}>
                <TextField
                    label="Search by name or email"
                    value={search}
                    onChange={e => setSearch(e.target.value)}
                    size="small"
                />
                <FormControl sx={{ minWidth: 120 }} size="small">
                    <InputLabel>Role</InputLabel>
                    <Select
                        value={roleFilter}
                        label="Role"
                        onChange={e => setRoleFilter(e.target.value)}
                    >
                        <MenuItem value="">All</MenuItem>
                        <MenuItem value="STAFF">Staff</MenuItem>
                        <MenuItem value="MANAGER">Manager</MenuItem>
                        <MenuItem value="ADMIN">Admin</MenuItem>
                    </Select>
                </FormControl>
                <FormControl sx={{ minWidth: 150 }} size="small">
                    <InputLabel>Department</InputLabel>
                    <Select
                        value={deptFilter}
                        label="Department"
                        onChange={e => setDeptFilter(e.target.value)}
                    >
                        <MenuItem value="">All</MenuItem>
                        {departments.map(dept => (
                            <MenuItem key={dept.id} value={dept.id}>{dept.name}</MenuItem>
                        ))}
                    </Select>
                </FormControl>
                <Button variant="contained" startIcon={<AddIcon />} sx={{ ml: 'auto' }} onClick={handleOpenAddDialog}>
                    Add User
                </Button>
                <Button variant="outlined" color="warning" onClick={handleFixDepartments}>
                    Fix Department Assignments
                </Button>
                <Button variant="outlined" color="info" onClick={handleSyncProfilePictures}>
                    Sync Profile Pictures
                </Button>
            </Box>
            {loading ? (
                <Box display="flex" justifyContent="center" alignItems="center" minHeight="200px">
                    <CircularProgress />
                </Box>
            ) : error ? (
                <Alert severity="error">{error}</Alert>
            ) : (
                <TableContainer component={Paper}>
                    <Table>
                        <TableHead>
                            <TableRow>
                                <TableCell padding="checkbox">
                                    <input
                                        type="checkbox"
                                        checked={selectedUsers.length > 0}
                                        onChange={e => {
                                            if (e.target.checked) {
                                                setSelectedUsers(users.map(user => user.id));
                                            } else {
                                                setSelectedUsers([]);
                                            }
                                        }}
                                    />
                                </TableCell>
                                <TableCell>Name</TableCell>
                                <TableCell>Email</TableCell>
                                <TableCell>Role</TableCell>
                                <TableCell>Department</TableCell>
                                <TableCell>Status</TableCell>
                                <TableCell align="right">Actions</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {filteredUsers.map((user) => (
                                <TableRow key={user.id}>
                                    <TableCell padding="checkbox">
                                        <input
                                            type="checkbox"
                                            checked={selectedUsers.includes(user.id)}
                                            onChange={e => {
                                                setSelectedUsers(sel =>
                                                    e.target.checked ? [...sel, user.id] : sel.filter(id => id !== user.id)
                                                );
                                            }}
                                        />
                                    </TableCell>
                                    <TableCell>{user.firstName} {user.lastName}</TableCell>
                                    <TableCell>{user.email}</TableCell>
                                    <TableCell>{user.role}</TableCell>
                                    <TableCell>{user.department ? user.department.name : '-'}</TableCell>
                                    <TableCell>
                                        {user.role === 'ADMIN' ? (
                                            <Chip label="Active" color="success" />
                                        ) : (
                                            user.isActive ? <Chip label="Active" color="success" /> : <Chip label="Inactive" color="default" />
                                        )}
                                    </TableCell>
                                    <TableCell align="right">
                                        {user.role !== 'ADMIN' && (
                                            <>
                                                <Button size="small" onClick={() => handleOpenViewDialog(user)}>View</Button>
                                                <IconButton size="small" color="primary" onClick={() => { setEditUser(user); setShowEditDialog(true); }}>
                                                    <EditIcon />
                                                </IconButton>
                                                <Button size="small" onClick={() => handleOpenAdjustDialog(user)}>Adjust Balance</Button>
                                                {user.isActive ? (
                                                    <IconButton size="small" color="error" onClick={() => { setDeactivateUser(user); setShowDeactivateDialog(true); }}>
                                                        <DeleteIcon />
                                                    </IconButton>
                                                ) : (
                                                    <Button size="small" color="success" onClick={() => { setActivateUser(user); setShowActivateDialog(true); }}>Activate</Button>
                                                )}
                                                <Button size="small" color="error" onClick={() => { setDeleteUser(user); setShowDeleteDialog(true); }}>Delete</Button>
                                            </>
                                        )}
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            )}
            {selectedUsers.length > 0 && (
                <Box sx={{ mb: 2, display: 'flex', gap: 2 }}>
                    <Button color="error" variant="outlined" onClick={() => setShowBulkDeactivate(true)}>
                        Bulk Deactivate
                    </Button>
                    <Button color="primary" variant="outlined" onClick={() => setShowBulkAssignDept(true)}>
                        Bulk Assign Department
                    </Button>
                    <Button color="secondary" variant="outlined" onClick={() => setShowBulkAdjust(true)}>
                        Bulk Adjust Balance
                    </Button>
                    {/* Bulk Activate button only if all selected users are inactive and not admins */}
                    {users.filter(u => selectedUsers.includes(u.id) && u.role !== 'ADMIN' && !u.isActive).length === selectedUsers.length && (
                        <Button color="success" variant="outlined" onClick={() => setShowBulkActivate(true)}>
                            Bulk Activate
                        </Button>
                    )}
                    <Typography sx={{ ml: 2 }}>{selectedUsers.length} selected</Typography>
                </Box>
            )}
            <Dialog open={openAddDialog} onClose={handleCloseAddDialog} maxWidth="sm" fullWidth>
                <DialogTitle>Add User</DialogTitle>
                <DialogContent>
                    <form onSubmit={handleAddUser} id="add-user-form">
                        <TextField
                            margin="normal"
                            fullWidth
                            label="First Name"
                            name="firstName"
                            value={userForm.firstName}
                            onChange={handleUserFormChange}
                            required
                        />
                        <TextField
                            margin="normal"
                            fullWidth
                            label="Last Name"
                            name="lastName"
                            value={userForm.lastName}
                            onChange={handleUserFormChange}
                            required
                        />
                        <TextField
                            margin="normal"
                            fullWidth
                            label="Email"
                            name="email"
                            type="email"
                            value={userForm.email}
                            onChange={handleUserFormChange}
                            required
                        />
                        <TextField
                            margin="normal"
                            fullWidth
                            label="Password"
                            name="password"
                            type="password"
                            value={userForm.password}
                            onChange={handleUserFormChange}
                            required
                        />
                        <FormControl fullWidth margin="normal">
                            <InputLabel>Role</InputLabel>
                            <Select
                                name="role"
                                value={userForm.role}
                                label="Role"
                                onChange={handleUserFormChange}
                            >
                                <MenuItem value="STAFF">Staff</MenuItem>
                                <MenuItem value="MANAGER">Manager</MenuItem>
                                <MenuItem value="ADMIN">Admin</MenuItem>
                            </Select>
                        </FormControl>
                        <FormControl fullWidth margin="normal">
                            <InputLabel>Department</InputLabel>
                            <Select
                                name="departmentId"
                                value={userForm.departmentId}
                                label="Department"
                                onChange={handleUserFormChange}
                            >
                                <MenuItem value="">None</MenuItem>
                                {departments.map((dept) => (
                                    <MenuItem key={dept.id} value={dept.id}>{dept.name}</MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                        {userError && <Alert severity="error" sx={{ mt: 2 }}>{userError}</Alert>}
                    </form>
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleCloseAddDialog} disabled={userLoading}>Cancel</Button>
                    <Button
                        variant="contained"
                        type="submit"
                        form="add-user-form"
                        disabled={userLoading}
                    >
                        {userLoading ? <CircularProgress size={20} /> : 'Add'}
                    </Button>
                </DialogActions>
            </Dialog>
            <Snackbar
                open={userSuccess}
                autoHideDuration={3000}
                onClose={() => setUserSuccess(false)}
                message="User added successfully!"
            />
            <Dialog open={showEditDialog} onClose={() => setShowEditDialog(false)} maxWidth="sm" fullWidth>
                <DialogTitle>Edit User</DialogTitle>
                <DialogContent>
                    <form onSubmit={handleEditUser} id="edit-user-form">
                        <TextField
                            margin="normal"
                            fullWidth
                            label="First Name"
                            name="firstName"
                            value={editForm.firstName}
                            onChange={handleEditFormChange}
                            required
                        />
                        <TextField
                            margin="normal"
                            fullWidth
                            label="Last Name"
                            name="lastName"
                            value={editForm.lastName}
                            onChange={handleEditFormChange}
                            required
                        />
                        <TextField
                            margin="normal"
                            fullWidth
                            label="Email"
                            name="email"
                            type="email"
                            value={editForm.email}
                            onChange={handleEditFormChange}
                            required
                        />
                        <FormControl fullWidth margin="normal">
                            <InputLabel>Role</InputLabel>
                            <Select
                                name="role"
                                value={editForm.role}
                                label="Role"
                                onChange={handleEditFormChange}
                            >
                                <MenuItem value="STAFF">Staff</MenuItem>
                                <MenuItem value="MANAGER">Manager</MenuItem>
                                <MenuItem value="ADMIN">Admin</MenuItem>
                            </Select>
                        </FormControl>
                        <FormControl fullWidth margin="normal">
                            <InputLabel>Department</InputLabel>
                            <Select
                                name="departmentId"
                                value={editForm.departmentId}
                                label="Department"
                                onChange={handleEditFormChange}
                            >
                                <MenuItem value="">None</MenuItem>
                                {departments.map((dept) => (
                                    <MenuItem key={dept.id} value={dept.id}>{dept.name}</MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                        {editError && <Alert severity="error" sx={{ mt: 2 }}>{editError}</Alert>}
                    </form>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setShowEditDialog(false)} disabled={editLoading}>Cancel</Button>
                    <Button
                        variant="contained"
                        type="submit"
                        form="edit-user-form"
                        disabled={editLoading}
                    >
                        {editLoading ? <CircularProgress size={20} /> : 'Save'}
                    </Button>
                </DialogActions>
            </Dialog>
            <Dialog open={showDeactivateDialog} onClose={() => setShowDeactivateDialog(false)}>
                <DialogTitle>Deactivate User</DialogTitle>
                <DialogContent>
                    <Typography>Are you sure you want to deactivate this user?</Typography>
                    {deactivateError && <Alert severity="error" sx={{ mt: 2 }}>{deactivateError}</Alert>}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setShowDeactivateDialog(false)} disabled={deactivateLoading}>Cancel</Button>
                    <Button color="error" variant="contained" onClick={handleDeactivateUser} disabled={deactivateLoading}>
                        {deactivateLoading ? <CircularProgress size={20} /> : 'Deactivate'}
                    </Button>
                </DialogActions>
            </Dialog>
            <Dialog open={showActivateDialog} onClose={() => setShowActivateDialog(false)}>
                <DialogTitle>Activate User</DialogTitle>
                <DialogContent>
                    <Typography>Are you sure you want to activate this user?</Typography>
                    {activateError && <Alert severity="error" sx={{ mt: 2 }}>{activateError}</Alert>}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setShowActivateDialog(false)} disabled={activateLoading}>Cancel</Button>
                    <Button color="success" variant="contained" onClick={handleActivateUser} disabled={activateLoading}>
                        {activateLoading ? <CircularProgress size={20} /> : 'Activate'}
                    </Button>
                </DialogActions>
            </Dialog>
            <Dialog open={showAdjustDialog} onClose={() => setShowAdjustDialog(false)} maxWidth="sm" fullWidth>
                <DialogTitle>Adjust Leave Balance</DialogTitle>
                <DialogContent>
                    <form onSubmit={handleSubmitAdjust} id="adjust-balance-form">
                        <FormControl fullWidth margin="normal">
                            <InputLabel>Leave Type</InputLabel>
                            <Select
                                name="leaveTypeId"
                                value={adjustForm.leaveTypeId}
                                label="Leave Type"
                                onChange={handleAdjustFormChange}
                                required
                            >
                                {leaveTypes.map((lt) => (
                                    <MenuItem key={lt.id} value={lt.id}>{lt.name}</MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                        {adjustForm.leaveTypeId && (
                            <Box sx={{ mb: 2 }}>
                                <Typography variant="body2" color="text.secondary">
                                    Current Balance: {
                                        userBalances.find(b => b.leaveTypeId === Number(adjustForm.leaveTypeId))?.remainingDays ?? 'N/A'
                                    } days
                                </Typography>
                            </Box>
                        )}
                        <TextField
                            margin="normal"
                            fullWidth
                            label="Adjustment Amount (days)"
                            name="adjustmentDays"
                            type="number"
                            value={adjustForm.adjustmentDays}
                            onChange={handleAdjustFormChange}
                            required
                        />
                        <TextField
                            margin="normal"
                            fullWidth
                            label="Reason (optional)"
                            name="reason"
                            value={adjustForm.reason}
                            onChange={handleAdjustFormChange}
                        />
                        {adjustError && <Alert severity="error" sx={{ mt: 2 }}>{adjustError}</Alert>}
                        {adjustSuccess && <Alert severity="success" sx={{ mt: 2 }}>Balance adjusted successfully!</Alert>}
                    </form>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setShowAdjustDialog(false)} disabled={adjustLoading}>Cancel</Button>
                    <Button variant="contained" type="submit" form="adjust-balance-form" disabled={adjustLoading}>
                        {adjustLoading ? <CircularProgress size={20} /> : 'Adjust'}
                    </Button>
                </DialogActions>
            </Dialog>
            <Dialog open={showViewDialog} onClose={() => setShowViewDialog(false)} maxWidth="md" fullWidth>
                <DialogTitle>User Details</DialogTitle>
                <DialogContent>
                    {userDetailsLoading ? (
                        <Box display="flex" justifyContent="center" alignItems="center" minHeight="200px">
                            <CircularProgress />
                        </Box>
                    ) : userDetailsError ? (
                        <Alert severity="error">{userDetailsError}</Alert>
                    ) : userDetails ? (
                        <>
                            <Box sx={{ mb: 2 }}>
                                <Typography variant="h6">{userDetails.firstName} {userDetails.lastName}</Typography>
                                <Typography variant="body2">Email: {userDetails.email}</Typography>
                                <Typography variant="body2">Role: {userDetails.role}</Typography>
                                <Typography variant="body2">Department: {userDetails.department ? userDetails.department.name : '-'}</Typography>
                                <Typography variant="body2">Status: {userDetails.isActive ? 'Active' : 'Inactive'}</Typography>
                            </Box>
                            <Tabs value={userDetailsTab} onChange={(_, v) => setUserDetailsTab(v)} sx={{ mb: 2 }}>
                                <Tab label="Leave Balances" />
                                <Tab label="Leave History" />
                            </Tabs>
                            {userDetailsTab === 0 && (
                                <TableContainer component={Paper} sx={{ mb: 2 }}>
                                    <Table size="small">
                                        <TableHead>
                                            <TableRow>
                                                <TableCell>Leave Type</TableCell>
                                                <TableCell>Total Days</TableCell>
                                                <TableCell>Used Days</TableCell>
                                                <TableCell>Remaining Days</TableCell>
                                                <TableCell>Carried Over</TableCell>
                                                <TableCell>Year</TableCell>
                                            </TableRow>
                                        </TableHead>
                                        <TableBody>
                                            {userDetailsBalances.map((b) => (
                                                <TableRow key={b.id}>
                                                    <TableCell>{b.leaveTypeName || '-'}</TableCell>
                                                    <TableCell>{b.totalDays}</TableCell>
                                                    <TableCell>{b.usedDays}</TableCell>
                                                    <TableCell>{b.remainingDays}</TableCell>
                                                    <TableCell>{b.carriedOverDays}</TableCell>
                                                    <TableCell>{b.year}</TableCell>
                                                </TableRow>
                                            ))}
                                        </TableBody>
                                    </Table>
                                </TableContainer>
                            )}
                            {userDetailsTab === 1 && (
                                <TableContainer component={Paper} sx={{ mb: 2 }}>
                                    <Table size="small">
                                        <TableHead>
                                            <TableRow>
                                                <TableCell>Type</TableCell>
                                                <TableCell>Start</TableCell>
                                                <TableCell>End</TableCell>
                                                <TableCell>Status</TableCell>
                                                <TableCell>Reason</TableCell>
                                                <TableCell>Document</TableCell>
                                                <TableCell>Approved By</TableCell>
                                                <TableCell>Comments</TableCell>
                                            </TableRow>
                                        </TableHead>
                                        <TableBody>
                                            {userDetailsHistory.map((h) => (
                                                <TableRow key={h.id}>
                                                    <TableCell>{h.leaveTypeName || h.leaveType || '-'}</TableCell>
                                                    <TableCell>{h.startDate}</TableCell>
                                                    <TableCell>{h.endDate}</TableCell>
                                                    <TableCell>{h.status}</TableCell>
                                                    <TableCell>{h.reason}</TableCell>
                                                    <TableCell>
                                                        {h.documents && h.documents.length > 0
                                                            ? h.documents.map(doc => (
                                                                <a key={doc.id} href={doc.url} target="_blank" rel="noopener noreferrer" style={{ marginRight: 8 }}>
                                                                    {doc.fileName || 'View'}
                                                                </a>
                                                            ))
                                                            : '-'}
                                                    </TableCell>
                                                    <TableCell>{h.approver ? `${h.approver.firstName} ${h.approver.lastName}` : '-'}</TableCell>
                                                    <TableCell>{h.approvalComments || '-'}</TableCell>
                                                </TableRow>
                                            ))}
                                        </TableBody>
                                    </Table>
                                </TableContainer>
                            )}
                        </>
                    ) : null}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setShowViewDialog(false)}>Close</Button>
                </DialogActions>
            </Dialog>
            <Dialog open={showBulkDeactivate} onClose={() => setShowBulkDeactivate(false)}>
                <DialogTitle>Bulk Deactivate Users</DialogTitle>
                <DialogContent>
                    <Typography>Are you sure you want to deactivate {selectedUsers.length} users?</Typography>
                    {deactivateError && <Alert severity="error" sx={{ mt: 2 }}>{deactivateError}</Alert>}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setShowBulkDeactivate(false)} disabled={deactivateLoading}>Cancel</Button>
                    <Button color="error" variant="contained" onClick={handleBulkDeactivate} disabled={deactivateLoading}>
                        {deactivateLoading ? <CircularProgress size={20} /> : 'Deactivate'}
                    </Button>
                </DialogActions>
            </Dialog>
            <Dialog open={showBulkAssignDept} onClose={() => setShowBulkAssignDept(false)} maxWidth="sm" fullWidth>
                <DialogTitle>Bulk Assign Department</DialogTitle>
                <DialogContent>
                    <FormControl fullWidth margin="normal">
                        <InputLabel>Department</InputLabel>
                        <Select
                            value={bulkAssignDeptId}
                            label="Department"
                            onChange={e => setBulkAssignDeptId(e.target.value)}
                        >
                            <MenuItem value="">Select department</MenuItem>
                            {departments.map(dept => (
                                <MenuItem key={dept.id} value={dept.id}>{dept.name}</MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                    {bulkAssignError && <Alert severity="error" sx={{ mt: 2 }}>{bulkAssignError}</Alert>}
                    {bulkAssignSuccess && <Alert severity="success" sx={{ mt: 2 }}>Users assigned to department!</Alert>}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setShowBulkAssignDept(false)} disabled={bulkAssignLoading}>Cancel</Button>
                    <Button
                        variant="contained"
                        disabled={!bulkAssignDeptId || bulkAssignLoading}
                        onClick={async () => {
                            setBulkAssignLoading(true);
                            setBulkAssignError('');
                            setBulkAssignSuccess(false);
                            try {
                                await authApi.post(
                                    `/users/bulk-assign-department?departmentId=${bulkAssignDeptId}`,
                                    selectedUsers
                                );
                                setBulkAssignSuccess(true);
                                setShowBulkAssignDept(false);
                                setBulkAssignDeptId('');
                                setSelectedUsers([]);
                                // Refresh users
                                const res = await authApi.get('/users');
                                setUsers(res.data);
                            } catch (err) {
                                setBulkAssignError(err.response?.data || 'Failed to assign department');
                            } finally {
                                setBulkAssignLoading(false);
                            }
                        }}
                    >
                        {bulkAssignLoading ? 'Assigning...' : 'Assign'}
                    </Button>
                </DialogActions>
            </Dialog>
            <Dialog open={showDeleteDialog} onClose={() => setShowDeleteDialog(false)}>
                <DialogTitle>Delete User</DialogTitle>
                <DialogContent>
                    <Typography>Are you sure you want to permanently delete this user? This action cannot be undone.</Typography>
                    {deactivateError && <Alert severity="error" sx={{ mt: 2 }}>{deactivateError}</Alert>}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setShowDeleteDialog(false)} disabled={deactivateLoading}>Cancel</Button>
                    <Button color="error" variant="contained" onClick={handleDeleteUser} disabled={deactivateLoading}>
                        {deactivateLoading ? <CircularProgress size={20} /> : 'Delete'}
                    </Button>
                </DialogActions>
            </Dialog>
            <Dialog open={showBulkActivate} onClose={() => setShowBulkActivate(false)}>
                <DialogTitle>Bulk Activate Users</DialogTitle>
                <DialogContent>
                    <Typography>Are you sure you want to activate {selectedUsers.length} users?</Typography>
                    {activateError && <Alert severity="error" sx={{ mt: 2 }}>{activateError}</Alert>}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setShowBulkActivate(false)} disabled={activateLoading}>Cancel</Button>
                    <Button color="success" variant="contained" onClick={handleBulkActivate} disabled={activateLoading}>
                        {activateLoading ? <CircularProgress size={20} /> : 'Activate'}
                    </Button>
                </DialogActions>
            </Dialog>
            <Dialog open={showBulkAdjust} onClose={() => setShowBulkAdjust(false)} maxWidth="sm" fullWidth>
                <DialogTitle>Bulk Adjust Leave Balance</DialogTitle>
                <DialogContent>
                    <FormControl fullWidth margin="normal">
                        <InputLabel>Leave Type</InputLabel>
                        <Select
                            value={bulkAdjustLeaveTypeId}
                            label="Leave Type"
                            onChange={e => setBulkAdjustLeaveTypeId(e.target.value)}
                        >
                            <MenuItem value="">Select leave type</MenuItem>
                            {leaveTypes.map(lt => (
                                <MenuItem key={lt.id} value={lt.id}>{lt.name}</MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                    <TextField
                        label="Adjustment Amount (days)"
                        type="number"
                        value={bulkAdjustAmount}
                        onChange={e => setBulkAdjustAmount(e.target.value)}
                        fullWidth
                        margin="normal"
                        required
                    />
                    <TextField
                        label="Reason (optional)"
                        value={bulkAdjustReason}
                        onChange={e => setBulkAdjustReason(e.target.value)}
                        fullWidth
                        margin="normal"
                    />
                    {bulkAdjustError && <Alert severity="error" sx={{ mt: 2 }}>{bulkAdjustError}</Alert>}
                    {bulkAdjustSuccess && <Alert severity="success" sx={{ mt: 2 }}>Balances adjusted successfully!</Alert>}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setShowBulkAdjust(false)} disabled={bulkAdjustLoading}>Cancel</Button>
                    <Button
                        variant="contained"
                        disabled={!bulkAdjustLeaveTypeId || !bulkAdjustAmount || bulkAdjustLoading}
                        onClick={async () => {
                            setBulkAdjustLoading(true);
                            setBulkAdjustError('');
                            setBulkAdjustSuccess(false);
                            try {
                                await leaveApi.post(
                                    `/leave-balances/bulk-adjust`,
                                    {
                                        userIds: selectedUsers,
                                        leaveTypeId: bulkAdjustLeaveTypeId,
                                        adjustmentDays: bulkAdjustAmount,
                                        reason: bulkAdjustReason
                                    }
                                );
                                setBulkAdjustSuccess(true);
                                setShowBulkAdjust(false);
                                setBulkAdjustLeaveTypeId('');
                                setBulkAdjustAmount('');
                                setBulkAdjustReason('');
                                setSelectedUsers([]);
                                // Refresh users
                                const res = await authApi.get('/users');
                                setUsers(res.data);
                            } catch (err) {
                                setBulkAdjustError(err.response?.data || 'Failed to adjust balances');
                            } finally {
                                setBulkAdjustLoading(false);
                            }
                        }}
                    >
                        {bulkAdjustLoading ? 'Adjusting...' : 'Adjust'}
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default UserManagement; 