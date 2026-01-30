

import React, { useState, useRef } from "react";
import api from "./api/axios";
import "./App.css";

// --- Matrix Overlay Component ---
const MatrixOverlay = ({ linearData, width, height, pos, title, isLocked }) => {
  if (!linearData || !pos || width === 0) {
    return <div className="matrix-viewer empty">HOVER OR CLICK {title}</div>;
  }

  const radius = 2;
  const px = Math.floor(pos.x); 
  const py = Math.floor(pos.y);
  const grid = [];

  for (let dy = -radius; dy <= radius; dy++) {
    const row = [];
    for (let dx = -radius; dx <= radius; dx++) {
      const x = px + dx; 
      const y = py + dy;

      if (x >= 0 && x < width && y >= 0 && y < height) {
        const val = linearData[y * width + x];
        if (typeof val === 'object' && val !== null) {
          row.push(val);
        } else if (typeof val === 'number') {
          row.push({
            r: (val >> 16) & 0xFF,
            g: (val >> 8) & 0xFF,
            b: val & 0xFF
          });
        } else {
          row.push(null);
        }
      } else {
        row.push(null);
      }
    }
    grid.push(row);
  }

  return (
    <div className={`matrix-viewer ${isLocked ? 'locked' : ''}`}>
      <div className="matrix-header">
        <span className="matrix-title">{title}</span>
        <span className="matrix-coords">[{px}, {py}] {isLocked && "🔒"}</span>
      </div>
      <div className="matrix-grid">
        {grid.map((row, i) => (
          <div key={i} className="matrix-row">
            {row.map((pixel, j) => (
              <div key={j} className="matrix-cell rgb-cell" style={{ 
                backgroundColor: pixel ? `rgb(${pixel.r},${pixel.g},${pixel.b})` : '#111',
                color: (pixel && (pixel.r + pixel.g + pixel.b)/3 > 128) ? '#000' : '#fff',
                border: (i===2 && j===2) ? '2px solid #6366f1' : '1px solid #333'
              }}>
                {pixel ? (
                  <div className="rgb-values">
                    <span>{pixel.r}</span>
                    <span>{pixel.g}</span>
                    <span>{pixel.b}</span>
                  </div>
                ) : '--'}
              </div>
            ))}
          </div>
        ))}
      </div>
    </div>
  );
};

export default function App() {
  const [originalUrl, setOriginalUrl] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [originalBlob, setOriginalBlob] = useState(null);
  const [sourceLinear, setSourceLinear] = useState(null);
  const [resultLinear, setResultLinear] = useState(null);
  const [dimensions, setDimensions] = useState({ width: 0, height: 0 });
  const [resultDimensions, setResultDimensions] = useState({ width: 0, height: 0 });
  const [loading, setLoading] = useState(false);
  const [mousePos, setMousePos] = useState(null);
  const [lockedPos, setLockedPos] = useState(null);
  const [isSquare, setIsSquare] = useState(null);

  const [brightness, setBrightness] = useState(0);
  const [contrast, setContrast] = useState(0);
  const [blurIntensity, setBlurIntensity] = useState(0);
  const [sharpenIntensity, setSharpenIntensity] = useState(0);
  const [rotateAngle, setRotateAngle] = useState(0);
  const [zoom, setZoom] = useState(1);
  const [isGrayscaleMode, setIsGrayscaleMode] = useState(false);
  
  const [cropData, setCropData] = useState({ x: 0, y: 0, w: 0, h: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const [startPos, setStartPos] = useState({ x: 0, y: 0 });
  
  const fileInputRef = useRef(null);

  const handleUpload = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setOriginalBlob(file);
    const url = URL.createObjectURL(file);
    setOriginalUrl(url);

    const img = new Image();
    img.onload = async () => {
      setDimensions({ width: img.width, height: img.height });
      setResultDimensions({ width: img.width, height: img.height });
      setCropData({ x: 0, y: 0, w: 0, h: 0 });
      
      const fd = new FormData();
      fd.append("image", file);
      fd.append("grayscale", false);
      fd.append("level", 0);

      try {
        const res = await api.post("/brightness", fd);
        setSourceLinear(res.data.linear);
        setResultLinear(res.data.linear);
        checkIfSquare(file);
      } catch (err) { console.error(err); }
    };
    img.src = url;
    setPreviewUrl(null);
    setLockedPos(null);
    resetToolStates();
  };

  const handleToggleGrayscale = async () => {
    const newMode = !isGrayscaleMode;
    setIsGrayscaleMode(newMode);
    
    if (originalBlob) {
      setLoading(true);
      const fd = new FormData();
      fd.append("image", originalBlob);
      fd.append("grayscale", newMode);
      fd.append("level", 0); 

      try {
        const res = await api.post("/brightness", fd);
        setSourceLinear(res.data.linear);
        processImage('brightness', { level: brightness }); 
      } catch (err) { console.error(err); } 
      finally { setLoading(false); }
    }
  };

  const checkIfSquare = async (file) => {
    const fd = new FormData();
    fd.append("image", file);
    try {
      const res = await api.post("/is-square", fd);
      setIsSquare(res.data);
    } catch (err) { console.error(err); }
  };

  const resetToolStates = () => {
    setBrightness(0); setContrast(0); setBlurIntensity(0);
    setSharpenIntensity(0); setRotateAngle(0); setZoom(1);
    setIsGrayscaleMode(false); setCropData({ x: 0, y: 0, w: 0, h: 0 });
  };

  const handleResetAll = () => {
    if (!originalBlob) return;
    resetToolStates();
    setPreviewUrl(null);
    setResultLinear(null);
    setLockedPos(null);
    const img = new Image();
    img.onload = () => {
        setDimensions({ width: img.width, height: img.height });
        setResultDimensions({ width: img.width, height: img.height });
    };
    img.src = originalUrl;
  };

const processImage = async (endpoint, params = {}) => {
    if (!originalBlob) return;

    // --- BLUR RESET FIX ---
    // We only reset locally if:
    // 1. We are NOT in Grayscale mode (if we are, we need the backend's gray baseline)
    // 2. We are NOT zoomed (if we are, we need the backend's expanded matrix)
    // 3. The intensity is being set to 0
    if (!isGrayscaleMode && zoom === 1 && (endpoint === 'blur' || endpoint === 'sharpen') && params.intensity === 0) {
        setPreviewUrl(null);
        setResultLinear(null);
        setResultDimensions({ width: dimensions.width, height: dimensions.height });
        return;
    }

    setLoading(true);
    const fd = new FormData();
    fd.append("image", originalBlob);
    fd.append("grayscale", isGrayscaleMode); // Ensure grayscale state is always sent
    
    // Maintain current zoom scale for all filter operations
    if (endpoint !== 'zoom' && zoom !== 1) {
        fd.append("scale", zoom);
    }

    Object.entries(params).forEach(([key, val]) => fd.append(key, val));

    try {
      const res = await api.post(`${endpoint}`, fd);
      setPreviewUrl(`data:image/png;base64,${res.data.image}`);
      setResultLinear(res.data.linear);
      
      // Synchronize dimensions from the server response
      setResultDimensions({ 
          width: res.data.width, 
          height: res.data.height 
      });

      if (endpoint === 'crop') {
          setDimensions({ width: res.data.width, height: res.data.height });
      }
    } catch (err) { 
      console.error("Filter application failed:", err); 
    } finally { 
      setLoading(false); 
    }
  };

  const handleMouseDown = (e) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const x = Math.floor(e.clientX - rect.left);
    const y = Math.floor(e.clientY - rect.top);
    setStartPos({ x, y });
    setIsDragging(true);
  };

  const handleMouseMoveViewport = (e, isResult) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const x = Math.floor(e.clientX - rect.left);
    const y = Math.floor(e.clientY - rect.top);
    setMousePos({ x, y });

    if (isDragging && !isResult) {
      const left = Math.max(0, Math.min(startPos.x, x));
      const top = Math.max(0, Math.min(startPos.y, y));
      const width = Math.min(dimensions.width - left, Math.abs(x - startPos.x));
      const height = Math.min(dimensions.height - top, Math.abs(y - startPos.y));
      setCropData({ x: left, y: top, w: width, h: height });
    }
  };

  const toggleLock = () => {
    if (lockedPos) setLockedPos(null);
    else setLockedPos({ ...mousePos });
  };

  return (
    <div className="pixel-app">
      <header className="brand-header">
        <div className="logo">PIXEL<span>GREY</span>.v1</div>
        <div className="header-controls">
            {isSquare !== null && (
                <div className={`square-badge ${isSquare ? 'is-square' : 'not-square'}`}>
                    {isSquare ? "■ SQUARE_MATCH" : "▭ ASYMMETRIC"}
                </div>
            )}
            <button className={`btn-secondary ${isGrayscaleMode ? 'active' : ''}`} onClick={handleToggleGrayscale}>
                {isGrayscaleMode ? "MODE: GRAYSCALE" : "MODE: COLOR"}
            </button>
        </div>
        <div className="status-indicator" style={{color: loading ? 'orange' : '#10b981'}}>
            {loading ? "BUSY" : "SYSTEM_READY"}
        </div>
      </header>

      <nav className="toolbar">
        <div className="tool-unit">
          <span className="tool-label">Analysis</span>
          <div className="status-box">
             <div className="stat-row">
                <span>Aspect:</span>
                <span style={{color: isSquare ? '#10b981' : '#f87171'}}>{isSquare ? "1:1 Match" : "Non-Square"}</span>
             </div>
             <button className="btn-secondary" onClick={() => checkIfSquare(originalBlob)}>Re-Validate</button>
          </div>
        </div>

        <div className="tool-unit">
          <span className="tool-label">Source</span>
          <button onClick={() => fileInputRef.current.click()}>Upload</button>
          <input type="file" ref={fileInputRef} onChange={handleUpload} hidden />
        </div>

        <div className="tool-unit">
          <span className="tool-label">Adjustments</span>
          <div className="slider-group">
            <div className="slider-item">
                <span>Bri({brightness})</span>
                <input type="range" min="-100" max="100" value={brightness} onChange={e => setBrightness(+e.target.value)} onMouseUp={() => processImage('brightness', { level: brightness })} />
            </div>
            <div className="slider-item">
                <span>Con({contrast})</span>
                <input type="range" min="-100" max="100" value={contrast} onChange={e => setContrast(+e.target.value)} onMouseUp={() => processImage('contrast', { level: contrast })} />
            </div>
          </div>
        </div>

        <div className="tool-unit">
          <span className="tool-label">Filters</span>
          <div className="slider-group">
            <div className="slider-item">
                <span>Blur({blurIntensity})</span>
                <input type="range" min="0" max="20" value={blurIntensity} onChange={e => setBlurIntensity(+e.target.value)} onMouseUp={() => processImage('blur', { intensity: blurIntensity })} />
            </div>
            <div className="slider-item">
                <span>Sharp({sharpenIntensity})</span>
                <input type="range" min="0" max="20" value={sharpenIntensity} onChange={e => setSharpenIntensity(+e.target.value)} onMouseUp={() => processImage('sharpen', { intensity: sharpenIntensity })} />
            </div>
          </div>
        </div>

        <div className="tool-unit">
          <span className="tool-label">Transform</span>
          <div className="input-group">
            <input type="number" className="number-input" value={rotateAngle} onChange={(e) => setRotateAngle(e.target.value)} />
            <button onClick={() => processImage('rotate', { angle: rotateAngle })}>Rotate</button>
          </div>
          <div className="btn-group" style={{marginTop:'5px'}}>
            <button onClick={() => processImage('flip/horizontal')}>Flip H</button>
            <button onClick={() => processImage('flip/vertical')}>Flip V</button>
          </div>
        </div>

        <div className="tool-unit">
          <span className="tool-label">SubMatrix</span>
          <div className="crop-stats" style={{fontSize: '11px', color: '#6366f1', fontWeight: 'bold'}}>
            POS: {cropData.x}, {cropData.y} <br/> AREA: {cropData.w}x{cropData.h}
          </div>
          <button className="btn-primary" style={{width:'100%', marginTop:'5px'}} onClick={() => processImage('crop', cropData)}>Apply Crop</button>
        </div>

        <div className="tool-unit">
          <span className="tool-label">Zoom Result ({zoom}x)</span>
          <input type="range" min="1" max="4" step="1" value={zoom} onChange={e => setZoom(+e.target.value)} onMouseUp={() => processImage('zoom', { scale: zoom })} />
          <div className="btn-group" style={{marginTop:'5px'}}>
            <button className="btn-secondary" onClick={() => setLockedPos(null)}>Unlock</button>
            <button className="btn-secondary" onClick={handleResetAll}>Reset All</button>
          </div>
        </div>
      </nav>

      <main className="workspace">
        <section className="panel">
          <MatrixOverlay linearData={sourceLinear} width={dimensions.width} height={dimensions.height} pos={lockedPos || mousePos} title="SOURCE" isLocked={!!lockedPos} />
          <MatrixOverlay 
            linearData={resultLinear || sourceLinear} 
            width={resultDimensions.width} 
            height={resultDimensions.height} 
            pos={
              (lockedPos || mousePos) 
                ? { 
                    x: (lockedPos ? lockedPos.x : mousePos.x) * (resultDimensions.width / dimensions.width), 
                    y: (lockedPos ? lockedPos.y : mousePos.y) * (resultDimensions.height / dimensions.height) 
                  } 
                : null
            } 
            title="RESULT" 
            isLocked={!!lockedPos} 
          />
        </section>

        <section className="preview-panel">
          <div className="comparison-view">
            <div className="viewport-half">
              <span className="view-tag">ORIGINAL</span>
              <div className="viewport-container" style={{justifyContent:'flex-start', alignItems:'flex-start'}} 
                   onClick={toggleLock} 
                   onMouseDown={handleMouseDown}
                   onMouseMove={(e) => handleMouseMoveViewport(e, false)} 
                   onMouseUp={() => setIsDragging(false)}>
                {originalUrl && (
                  <img src={originalUrl} className="preview-img" draggable="false" alt="source" 
                       style={{ filter: isGrayscaleMode ? 'grayscale(100%)' : 'none', imageRendering: 'pixelated' }} />
                )}
                {isDragging && <div className="crop-selector-visual" style={{position:'absolute', border:'2px dashed #6366f1', backgroundColor:'rgba(99,102,241,0.2)', left:cropData.x, top:cropData.y, width:cropData.w, height:cropData.h, pointerEvents:'none'}} />}
              </div>
            </div>
            <div className="viewport-half">
              <span className="view-tag">RESULT</span>
              <div className="viewport-container" style={{justifyContent:'flex-start', alignItems:'flex-start'}} onClick={toggleLock} onMouseMove={(e) => handleMouseMoveViewport(e, true)}>
                {previewUrl ? (
                  <img src={previewUrl} className="preview-img" draggable="false" alt="result" style={{ imageRendering: 'pixelated' }} />
                ) : (
                  originalUrl && <img src={originalUrl} className="preview-img" draggable="false" alt="source-copy" 
                                      style={{ filter: isGrayscaleMode ? 'grayscale(100%)' : 'none', imageRendering: 'pixelated' }} />
                )}
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}


