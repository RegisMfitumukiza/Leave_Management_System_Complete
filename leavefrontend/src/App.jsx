import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { authApi } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import theme from '@/theme';
import { AuthProvider } from './contexts/AuthContext';
import ProtectedRoute from './components/auth/ProtectedRoute';
import RoleBasedRedirect from './components/auth/RoleBasedRedirect';
import TeamCalendar from './pages/admin/TeamCalendar';
import AdminProfile from './pages/admin/Profile';
import StaffLayout from './components/staff/StaffLayout';
// import Layout from './components/Layout';

// Auth Pages
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import OAuthSuccess from './pages/OAuthSuccess';
import AdminDashboard from './pages/admin/AdminDashboard';
import UserManagement from './pages/admin/UserManagement';
import DepartmentManagement from './pages/admin/DepartmentManagement';
import LeaveTypes from './pages/admin/LeaveTypes';
import Reports from './pages/admin/Reports';
import Holidays from './pages/admin/Holidays';
import Settings from './pages/admin/Settings';
import LeaveAnalytics from './pages/admin/LeaveAnalytics';
import ManagerDashboard from './pages/manager/ManagerDashboard';
import LeaveApprovals from './pages/manager/LeaveApprovals';
import MyLeave from './pages/manager/MyLeave';
import ManagerProfile from './pages/manager/Profile';
import ManagerReports from './pages/manager/Reports';
import ManagerDocuments from './pages/manager/Documents';
// Staff Pages
import StaffDashboard from './pages/staff/StaffDashboard';
import ApplyLeave from './pages/staff/ApplyLeave';
import LeaveHistory from './pages/staff/LeaveHistory';
import StaffProfile from './pages/staff/Profile';
// Placeholder imports for other sidebar pages

// Create a separate component for the authenticated app content
const AuthenticatedApp = () => {
  const { user, login, logout } = useAuth();

  useEffect(() => {
    const refreshToken = async () => {
      const storedRefreshToken = localStorage.getItem('refreshToken');
      if (!storedRefreshToken) {
        logout();
        return;
      }
      try {
        const res = await authApi.post('/refresh-token', { refreshToken: storedRefreshToken });
        login(res.data);
        if (res.data.refreshToken) {
          localStorage.setItem('refreshToken', res.data.refreshToken);
        }
      } catch (err) {
        logout();
      }
    };
    refreshToken();
  }, []);

  return (
    <Router>
      <Routes>
        {/* Public Routes */}
        <Route path="/auth/login" element={<LoginPage />} />
        <Route path="/auth/register" element={<RegisterPage />} />
        <Route path="/auth/redirect" element={<RoleBasedRedirect />} />
        <Route path="/auth/oauth-success" element={<OAuthSuccess />} />
        {/* Admin Dashboard and Sidebar Pages */}
        <Route path="/admin/dashboard" element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <AdminDashboard />
          </ProtectedRoute>
        } />
        <Route path="/admin/users" element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <UserManagement />
          </ProtectedRoute>
        } />
        <Route path="/admin/departments" element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <DepartmentManagement />
          </ProtectedRoute>
        } />
        <Route path="/admin/leave-types" element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <LeaveTypes />
          </ProtectedRoute>
        } />
        <Route path="/admin/holidays" element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <Holidays />
          </ProtectedRoute>
        } />
        <Route path="/admin/reports" element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <Reports />
          </ProtectedRoute>
        } />
        <Route path="/admin/team-calendar" element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <TeamCalendar />
          </ProtectedRoute>
        } />
        <Route path="/admin/leave-analytics" element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <LeaveAnalytics />
          </ProtectedRoute>
        } />
        <Route path="/admin/settings" element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <Settings />
          </ProtectedRoute>
        } />
        <Route path="/admin/profile" element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <AdminProfile />
          </ProtectedRoute>
        } />
        <Route path="/manager/dashboard" element={
          <ProtectedRoute allowedRoles={['MANAGER']}>
            <ManagerDashboard />
          </ProtectedRoute>
        } />
        <Route path="/manager/approvals" element={
          <ProtectedRoute allowedRoles={['MANAGER']}>
            <LeaveApprovals />
          </ProtectedRoute>
        } />
        <Route path="/manager/team-calendar" element={
          <ProtectedRoute allowedRoles={['MANAGER']}>
            <TeamCalendar />
          </ProtectedRoute>
        } />
        <Route path="/manager/my-leave" element={
          <ProtectedRoute allowedRoles={['MANAGER']}>
            <MyLeave />
          </ProtectedRoute>
        } />
        <Route path="/manager/profile" element={
          <ProtectedRoute allowedRoles={['MANAGER']}>
            <ManagerProfile />
          </ProtectedRoute>
        } />
        <Route path="/manager/reports" element={
          <ProtectedRoute allowedRoles={['MANAGER']}>
            <ManagerReports />
          </ProtectedRoute>
        } />
        <Route path="/manager/documents" element={
          <ProtectedRoute allowedRoles={['MANAGER']}>
            <ManagerDocuments />
          </ProtectedRoute>
        } />
        {/* Staff Routes */}
        <Route path="/staff/*" element={
          <ProtectedRoute allowedRoles={['STAFF']}>
            <StaffLayout>
              <Routes>
                <Route index element={<Navigate to="dashboard" replace />} />
                <Route path="dashboard" element={<StaffDashboard />} />
                <Route path="apply-leave" element={<ApplyLeave />} />
                <Route path="leave-history" element={<LeaveHistory />} />
                <Route path="profile" element={<StaffProfile />} />
                <Route path="leave-details/:id" element={
                  <Navigate to="dashboard" replace />
                } />
                <Route path="*" element={<Navigate to="dashboard" replace />} />
              </Routes>
            </StaffLayout>
          </ProtectedRoute>
        } />
        {/* Default Redirect */}
        <Route path="/" element={<Navigate to="/auth/login" replace />} />
        <Route path="*" element={<Navigate to="/auth/login" replace />} />
      </Routes>
    </Router>
  );
};

const App = () => {
  return (
    <AuthProvider>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <AuthenticatedApp />
      </ThemeProvider>
    </AuthProvider>
  );
};

export default App;
