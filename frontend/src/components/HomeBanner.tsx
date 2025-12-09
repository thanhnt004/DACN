import { useEffect, useState } from 'react';
import { Box, IconButton } from '@mui/material';
import { ArrowBackIos, ArrowForwardIos } from '@mui/icons-material';
import { getPublicBanners, Banner } from '../api/admin/banner';
import { Link } from 'react-router-dom';

export default function HomeBanner() {
  const [banners, setBanners] = useState<Banner[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadBanners();
  }, []);

  useEffect(() => {
    if (banners.length === 0) return;

    const interval = setInterval(() => {
      setCurrentIndex((prev) => (prev + 1) % banners.length);
    }, 5000); // Auto-slide every 5 seconds

    return () => clearInterval(interval);
  }, [banners.length]);

  const loadBanners = async () => {
    try {
      const data = await getPublicBanners();
      setBanners(data);
    } catch (error) {
      console.error('Failed to load banners:', error);
    } finally {
      setLoading(false);
    }
  };

  const handlePrev = () => {
    setCurrentIndex((prev) => (prev - 1 + banners.length) % banners.length);
  };

  const handleNext = () => {
    setCurrentIndex((prev) => (prev + 1) % banners.length);
  };

  const handleDotClick = (index: number) => {
    setCurrentIndex(index);
  };

  if (loading || banners.length === 0) {
    return null;
  }

  const currentBanner = banners[currentIndex];

  return (
    <Box
      sx={{
        position: 'relative',
        width: '100%',
        height: { xs: 200, sm: 300, md: 400, lg: 500 },
        overflow: 'hidden',
        bgcolor: 'grey.100',
      }}
    >
      {/* Banner Image */}
      <Box
        component={currentBanner.linkUrl ? Link : 'div'}
        to={currentBanner.linkUrl || ''}
        sx={{
          display: 'block',
          width: '100%',
          height: '100%',
          textDecoration: 'none',
        }}
      >
        <Box
          component="img"
          src={currentBanner.imageUrl}
          alt={currentBanner.title}
          sx={{
            width: '100%',
            height: '100%',
            objectFit: 'cover',
          }}
        />
      </Box>

      {/* Navigation Arrows */}
      {banners.length > 1 && (
        <>
          <IconButton
            onClick={handlePrev}
            sx={{
              position: 'absolute',
              left: 16,
              top: '50%',
              transform: 'translateY(-50%)',
              bgcolor: 'rgba(255, 255, 255, 0.8)',
              '&:hover': {
                bgcolor: 'rgba(255, 255, 255, 0.95)',
              },
            }}
          >
            <ArrowBackIos sx={{ ml: 0.5 }} />
          </IconButton>

          <IconButton
            onClick={handleNext}
            sx={{
              position: 'absolute',
              right: 16,
              top: '50%',
              transform: 'translateY(-50%)',
              bgcolor: 'rgba(255, 255, 255, 0.8)',
              '&:hover': {
                bgcolor: 'rgba(255, 255, 255, 0.95)',
              },
            }}
          >
            <ArrowForwardIos />
          </IconButton>
        </>
      )}

      {/* Dots Indicator */}
      {banners.length > 1 && (
        <Box
          sx={{
            position: 'absolute',
            bottom: 16,
            left: '50%',
            transform: 'translateX(-50%)',
            display: 'flex',
            gap: 1,
          }}
        >
          {banners.map((_, index) => (
            <Box
              key={index}
              onClick={() => handleDotClick(index)}
              sx={{
                width: 12,
                height: 12,
                borderRadius: '50%',
                bgcolor: index === currentIndex ? 'white' : 'rgba(255, 255, 255, 0.5)',
                cursor: 'pointer',
                transition: 'all 0.3s',
                '&:hover': {
                  bgcolor: 'white',
                },
              }}
            />
          ))}
        </Box>
      )}

      {/* Banner Info Overlay */}
      {(currentBanner.title || currentBanner.description) && (
        <Box
          sx={{
            position: 'absolute',
            bottom: 0,
            left: 0,
            right: 0,
            background: 'linear-gradient(to top, rgba(0,0,0,0.7), transparent)',
            color: 'white',
            p: 3,
          }}
        >
          {currentBanner.title && (
            <Box
              component="h2"
              sx={{
                m: 0,
                mb: 1,
                fontSize: { xs: '1.25rem', md: '1.75rem' },
                fontWeight: 'bold',
              }}
            >
              {currentBanner.title}
            </Box>
          )}
          {currentBanner.description && (
            <Box
              component="p"
              sx={{
                m: 0,
                fontSize: { xs: '0.875rem', md: '1rem' },
                opacity: 0.9,
              }}
            >
              {currentBanner.description}
            </Box>
          )}
        </Box>
      )}
    </Box>
  );
}
