import {
  Add as AddIcon,
  Delete as DeleteIcon,
  DragIndicator as DragIcon,
  Edit as EditIcon,
} from '@mui/icons-material';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CardMedia,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  IconButton,
  Paper,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { useEffect, useState } from 'react';
import { toast } from 'react-hot-toast';
import { resolveErrorMessage } from '../../lib/problemDetails';
import {
  Banner,
  BannerCreateRequest,
  BannerUpdateRequest,
  createBanner,
  deleteBanner,
  getAllBanners,
  reorderBanners,
  toggleBannerStatus,
  updateBanner,
} from '../../api/admin/banner';
import ImageUploadZone from '../../components/layout/ImageUploadZone';

export default function BannerManager() {
  const [banners, setBanners] = useState<Banner[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingBanner, setEditingBanner] = useState<Banner | null>(null);
  const [formData, setFormData] = useState<BannerCreateRequest>({
    title: '',
    description: '',
    imageUrl: '',
    linkUrl: '',
    isActive: true,
    displayOrder: 0,
  });

  useEffect(() => {
    loadBanners();
  }, []);

  const loadBanners = async () => {
    try {
      setLoading(true);
      const data = await getAllBanners();
      setBanners(data);
      setError(null);
    } catch (err: any) {
      const errorMsg = resolveErrorMessage(err, 'Không thể tải danh sách banner');
      setError(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (banner?: Banner) => {
    if (banner) {
      setEditingBanner(banner);
      setFormData({
        title: banner.title,
        description: banner.description || '',
        imageUrl: banner.imageUrl,
        linkUrl: banner.linkUrl || '',
        isActive: banner.isActive,
        displayOrder: banner.displayOrder,
        startDate: banner.startDate,
        endDate: banner.endDate,
      });
    } else {
      setEditingBanner(null);
      setFormData({
        title: '',
        description: '',
        imageUrl: '',
        linkUrl: '',
        isActive: true,
        displayOrder: banners.length,
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingBanner(null);
    setFormData({
      title: '',
      description: '',
      imageUrl: '',
      linkUrl: '',
      isActive: true,
      displayOrder: 0,
    });
  };

  const handleSubmit = async () => {
    try {
      setLoading(true);
      if (editingBanner) {
        await updateBanner(editingBanner.id, formData as BannerUpdateRequest);
      } else {
        await createBanner(formData);
      }
      await loadBanners();
      handleCloseDialog();
      setError(null);
    } catch (err: any) {
      const errorMsg = resolveErrorMessage(err, 'Không thể lưu banner');
      setError(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Bạn có chắc muốn xóa banner này?')) return;
    
    try {
      setLoading(true);
      await deleteBanner(id);
      await loadBanners();
      setError(null);
    } catch (err: any) {
      const errorMsg = resolveErrorMessage(err, 'Không thể xóa banner');
      setError(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const handleToggleStatus = async (id: string) => {
    try {
      setLoading(true);
      await toggleBannerStatus(id);
      await loadBanners();
      setError(null);
    } catch (err: any) {
      const errorMsg = resolveErrorMessage(err, 'Không thể thay đổi trạng thái');
      setError(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const handleDragStart = (e: React.DragEvent, index: number) => {
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/plain', index.toString());
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
  };

  const handleDrop = async (e: React.DragEvent, dropIndex: number) => {
    e.preventDefault();
    const dragIndex = parseInt(e.dataTransfer.getData('text/plain'));
    
    if (dragIndex === dropIndex) return;

    const newBanners = [...banners];
    const [removed] = newBanners.splice(dragIndex, 1);
    newBanners.splice(dropIndex, 0, removed);

    setBanners(newBanners);

    try {
      setLoading(true);
      await reorderBanners(newBanners.map(b => b.id));
      await loadBanners();
      setError(null);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Không thể sắp xếp lại banner');
      await loadBanners(); // Reload to get correct order
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h4">Quản lý Banner</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
        >
          Thêm Banner
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell width="50">Thứ tự</TableCell>
              <TableCell width="100">Hình ảnh</TableCell>
              <TableCell>Tiêu đề</TableCell>
              <TableCell>Mô tả</TableCell>
              <TableCell width="100">Trạng thái</TableCell>
              <TableCell width="150">Thao tác</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {banners.map((banner, index) => (
              <TableRow
                key={banner.id}
                draggable
                onDragStart={(e) => handleDragStart(e, index)}
                onDragOver={handleDragOver}
                onDrop={(e) => handleDrop(e, index)}
                sx={{ cursor: 'grab', '&:active': { cursor: 'grabbing' } }}
              >
                <TableCell>
                  <Box sx={{ display: 'flex', alignItems: 'center' }}>
                    <DragIcon sx={{ mr: 1, color: 'text.secondary' }} />
                    {banner.displayOrder}
                  </Box>
                </TableCell>
                <TableCell>
                  <Card sx={{ width: 80, height: 45 }}>
                    <CardMedia
                      component="img"
                      image={banner.imageUrl}
                      alt={banner.title}
                      sx={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    />
                  </Card>
                </TableCell>
                <TableCell>
                  <Typography variant="body2" fontWeight="bold">
                    {banner.title}
                  </Typography>
                  {banner.linkUrl && (
                    <Typography variant="caption" color="text.secondary">
                      {banner.linkUrl}
                    </Typography>
                  )}
                </TableCell>
                <TableCell>
                  <Typography variant="body2" color="text.secondary">
                    {banner.description || '-'}
                  </Typography>
                </TableCell>
                <TableCell>
                  <FormControlLabel
                    control={
                      <Switch
                        checked={banner.isActive}
                        onChange={() => handleToggleStatus(banner.id)}
                        disabled={loading}
                      />
                    }
                    label={banner.isActive ? 'Hiển thị' : 'Ẩn'}
                  />
                </TableCell>
                <TableCell>
                  <IconButton
                    size="small"
                    onClick={() => handleOpenDialog(banner)}
                    disabled={loading}
                  >
                    <EditIcon />
                  </IconButton>
                  <IconButton
                    size="small"
                    color="error"
                    onClick={() => handleDelete(banner.id)}
                    disabled={loading}
                  >
                    <DeleteIcon />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="md" fullWidth>
        <DialogTitle>
          {editingBanner ? 'Chỉnh sửa Banner' : 'Thêm Banner mới'}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 2 }}>
            <TextField
              label="Tiêu đề"
              fullWidth
              required
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
            />
            <TextField
              label="Mô tả"
              fullWidth
              multiline
              rows={2}
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            />
            <ImageUploadZone
                id="banner-image-upload"
                description="Kéo và thả ảnh vào đây, hoặc nhấn để chọn ảnh"
                buttonLabel="Chọn ảnh banner"
                uploadType="banner"
                autoUpload={true}
                multiple={false}
                onUploadComplete={(urls) => {
                    if (urls.length > 0) {
                        setFormData({ ...formData, imageUrl: urls[0] });
                    }
                }}
            />
            <TextField
              label="URL hình ảnh"
              fullWidth
              required
              value={formData.imageUrl}
              onChange={(e) => setFormData({ ...formData, imageUrl: e.target.value })}
            />
            {formData.imageUrl && (
              <Card sx={{ maxWidth: 400 }}>
                <CardMedia
                  component="img"
                  image={formData.imageUrl}
                  alt="Preview"
                  sx={{ maxHeight: 200, objectFit: 'contain' }}
                />
              </Card>
            )}
            <TextField
              label="URL liên kết"
              fullWidth
              value={formData.linkUrl}
              onChange={(e) => setFormData({ ...formData, linkUrl: e.target.value })}
            />
            <TextField
              label="Thứ tự hiển thị"
              type="number"
              fullWidth
              value={formData.displayOrder}
              onChange={(e) => setFormData({ ...formData, displayOrder: parseInt(e.target.value) })}
            />
            <FormControlLabel
              control={
                <Switch
                  checked={formData.isActive}
                  onChange={(e) => setFormData({ ...formData, isActive: e.target.checked })}
                />
              }
              label="Hiển thị banner"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Hủy</Button>
          <Button
            onClick={handleSubmit}
            variant="contained"
            disabled={loading || !formData.title || !formData.imageUrl}
          >
            {editingBanner ? 'Cập nhật' : 'Tạo mới'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
