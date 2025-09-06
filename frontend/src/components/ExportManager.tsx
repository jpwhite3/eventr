import React, { useState } from 'react';
import { ExportManager, ExportOptions, ExportProgress, ChartExporter } from '../utils/exportUtils';

interface ExportManagerProps {
  analyticsData: any;
  eventName?: string;
  dateRange?: string;
}

interface ExportState {
  isExporting: boolean;
  progress: ExportProgress | null;
  error: string | null;
  format: 'pdf' | 'excel' | 'csv' | 'png' | 'svg';
  includeCharts: boolean;
  showDateRange: boolean;
  customFilename: string;
}

const ExportManagerComponent: React.FC<ExportManagerProps> = ({ 
  analyticsData, 
  eventName = 'Event Analytics',
  dateRange = 'all' 
}) => {
  const [state, setState] = useState<ExportState>({
    isExporting: false,
    progress: null,
    error: null,
    format: 'pdf',
    includeCharts: true,
    showDateRange: true,
    customFilename: ''
  });

  const generateFilename = (format: string) => {
    if (state.customFilename.trim()) {
      return `${state.customFilename}.${format}`;
    }

    const eventSlug = eventName.toLowerCase().replace(/[^a-z0-9]/g, '-');
    const dateSlug = state.showDateRange ? `_${dateRange}` : '';
    const timestamp = new Date().toISOString().split('T')[0];
    
    return `${eventSlug}_analytics${dateSlug}_${timestamp}.${format}`;
  };

  const handleExport = async () => {
    if (!analyticsData) {
      setState(prev => ({ ...prev, error: 'No analytics data available' }));
      return;
    }

    setState(prev => ({ 
      ...prev, 
      isExporting: true, 
      error: null, 
      progress: null 
    }));

    try {
      const filename = generateFilename(state.format);
      const options: ExportOptions = {
        format: state.format,
        filename,
        includeCharts: state.includeCharts,
        dateRange,
        eventName
      };

      if (state.format === 'png' || state.format === 'svg') {
        // Handle chart exports differently
        await handleChartExport(filename);
      } else {
        await ExportManager.exportAnalytics(
          analyticsData,
          options,
          (progress) => setState(prev => ({ ...prev, progress }))
        );
      }

      setState(prev => ({ 
        ...prev, 
        isExporting: false, 
        progress: { step: 'Export completed successfully!', progress: 100, total: 100 }
      }));

      // Clear progress after 2 seconds
      setTimeout(() => {
        setState(prev => ({ ...prev, progress: null }));
      }, 2000);

    } catch (error) {
      console.error('Export failed:', error);
      setState(prev => ({ 
        ...prev, 
        isExporting: false,
        error: error instanceof Error ? error.message : 'Export failed'
      }));
    }
  };

  const handleChartExport = async (filename: string) => {
    // Export multiple charts for comprehensive chart export
    const charts = [
      { id: 'registration-trend-chart', name: 'registration_timeline' },
      { id: 'checkin-methods-chart', name: 'checkin_methods' },
      { id: 'session-analytics-chart', name: 'session_analytics' }
    ];

    let exportedCount = 0;
    
    const exportChart = async (chart: { id: string; name: string }, index: number) => {
      try {
        const chartFilename = filename.replace('.', `_${chart.name}.`);
        
        if (state.format === 'png') {
          await ChartExporter.exportChartAsPNG(chart.id, chartFilename);
        } else {
          ChartExporter.exportChartAsSVG(chart.id, chartFilename);
        }
        exportedCount++;
      } catch (error) {
        console.warn(`Could not export chart ${chart.id}:`, error);
      }

      setState(prev => ({
        ...prev,
        progress: {
          step: `Exporting charts... (${exportedCount}/${charts.length})`,
          progress: exportedCount,
          total: charts.length
        }
      }));
    };

    for (let i = 0; i < charts.length; i++) {
      await exportChart(charts[i], i);
    }

    if (exportedCount === 0) {
      throw new Error('No charts could be exported. Ensure charts are visible on the page.');
    }
  };

  const getFormatDescription = () => {
    switch (state.format) {
      case 'pdf':
        return 'Comprehensive report with charts and tables';
      case 'excel':
        return 'Spreadsheet with multiple data sheets';
      case 'csv':
        return 'Raw data in comma-separated format';
      case 'png':
        return 'High-resolution chart images';
      case 'svg':
        return 'Scalable vector chart graphics';
      default:
        return '';
    }
  };

  const getProgressPercentage = () => {
    if (!state.progress) return 0;
    return Math.round((state.progress.progress / state.progress.total) * 100);
  };

  return (
    <div className="export-manager">
      <div className="card">
        <div className="card-header d-flex justify-content-between align-items-center">
          <h5 className="mb-0">📥 Export Analytics</h5>
          <div className="text-muted small">
            {analyticsData ? '✅ Data Ready' : '⏳ No Data'}
          </div>
        </div>
        
        <div className="card-body">
          {/* Export Format Selection */}
          <div className="mb-3">
            <label className="form-label fw-bold">Export Format</label>
            <div className="row">
              {[
                { value: 'pdf', label: 'PDF Report', icon: '📄' },
                { value: 'excel', label: 'Excel Workbook', icon: '📊' },
                { value: 'csv', label: 'CSV Data', icon: '📋' },
                { value: 'png', label: 'Chart Images', icon: '🖼️' },
                { value: 'svg', label: 'Vector Graphics', icon: '🎨' }
              ].map(format => (
                <div key={format.value} className="col-md-4 col-6 mb-2">
                  <div 
                    className={`export-format-option ${state.format === format.value ? 'active' : ''}`}
                    onClick={() => setState(prev => ({ ...prev, format: format.value as any }))}
                  >
                    <div className="format-icon">{format.icon}</div>
                    <div className="format-label">{format.label}</div>
                  </div>
                </div>
              ))}
            </div>
            <div className="form-text">{getFormatDescription()}</div>
          </div>

          {/* Export Options */}
          <div className="mb-3">
            <label className="form-label fw-bold">Export Options</label>
            
            {(state.format === 'pdf' || state.format === 'png' || state.format === 'svg') && (
              <div className="form-check mb-2">
                <input
                  className="form-check-input"
                  type="checkbox"
                  checked={state.includeCharts}
                  onChange={(e) => setState(prev => ({ ...prev, includeCharts: e.target.checked }))}
                  id="includeCharts"
                />
                <label className="form-check-label" htmlFor="includeCharts">
                  Include Charts {state.format === 'pdf' ? '(in PDF)' : '(as separate files)'}
                </label>
              </div>
            )}

            <div className="form-check mb-2">
              <input
                className="form-check-input"
                type="checkbox"
                checked={state.showDateRange}
                onChange={(e) => setState(prev => ({ ...prev, showDateRange: e.target.checked }))}
                id="showDateRange"
              />
              <label className="form-check-label" htmlFor="showDateRange">
                Include date range in filename ({dateRange})
              </label>
            </div>
          </div>

          {/* Custom Filename */}
          <div className="mb-3">
            <label className="form-label fw-bold">Custom Filename (Optional)</label>
            <input
              type="text"
              className="form-control"
              placeholder={`Default: ${generateFilename(state.format)}`}
              value={state.customFilename}
              onChange={(e) => setState(prev => ({ ...prev, customFilename: e.target.value }))}
            />
            <div className="form-text">
              Preview: {generateFilename(state.format)}
            </div>
          </div>

          {/* Error Display */}
          {state.error && (
            <div className="alert alert-danger">
              <strong>Export Failed:</strong> {state.error}
            </div>
          )}

          {/* Progress Display */}
          {state.progress && (
            <div className="mb-3">
              <div className="d-flex justify-content-between align-items-center mb-1">
                <small className="text-muted">{state.progress.step}</small>
                <small className="text-muted">{getProgressPercentage()}%</small>
              </div>
              <div className="progress">
                <div 
                  className="progress-bar progress-bar-animated" 
                  style={{ width: `${getProgressPercentage()}%` }}
                />
              </div>
            </div>
          )}

          {/* Export Button */}
          <div className="d-grid">
            <button
              className="btn btn-primary btn-lg"
              disabled={!analyticsData || state.isExporting}
              onClick={handleExport}
            >
              {state.isExporting ? (
                <>
                  <div className="spinner-border spinner-border-sm me-2" role="status">
                    <span className="visually-hidden">Exporting...</span>
                  </div>
                  Exporting...
                </>
              ) : (
                <>
                  📥 Export {state.format.toUpperCase()}
                </>
              )}
            </button>
          </div>

          {/* Export Info */}
          <div className="mt-3 p-3 bg-light rounded">
            <h6 className="fw-bold mb-2">📋 What's Included</h6>
            <div className="small">
              {state.format === 'pdf' && (
                <ul className="mb-0">
                  <li>Executive summary with key metrics</li>
                  <li>Session performance tables</li>
                  {state.includeCharts && <li>Registration timeline and check-in method charts</li>}
                  <li>Professional formatting with headers and footers</li>
                </ul>
              )}
              {state.format === 'excel' && (
                <ul className="mb-0">
                  <li>Key Metrics sheet with summary statistics</li>
                  <li>Session Analytics sheet with detailed performance data</li>
                  <li>Registration Timeline sheet with daily registration data</li>
                  <li>Check-in Methods sheet with method breakdown</li>
                </ul>
              )}
              {state.format === 'csv' && (
                <ul className="mb-0">
                  <li>Raw data in comma-separated format</li>
                  <li>Multiple data sheets combined in one file</li>
                  <li>Perfect for data analysis and custom reporting</li>
                </ul>
              )}
              {(state.format === 'png' || state.format === 'svg') && (
                <ul className="mb-0">
                  <li>High-resolution chart exports</li>
                  <li>Separate files for each chart type</li>
                  <li>Perfect for presentations and reports</li>
                  {state.format === 'svg' && <li>Scalable vector format for print quality</li>}
                </ul>
              )}
            </div>
          </div>
        </div>
      </div>

      <style>{`
        .export-format-option {
          border: 2px solid #e9ecef;
          border-radius: 8px;
          padding: 12px;
          text-align: center;
          cursor: pointer;
          transition: all 0.2s ease;
          height: 80px;
          display: flex;
          flex-direction: column;
          justify-content: center;
          align-items: center;
        }

        .export-format-option:hover {
          border-color: #0d6efd;
          background-color: #f8f9fa;
        }

        .export-format-option.active {
          border-color: #0d6efd;
          background-color: #e7f3ff;
        }

        .format-icon {
          font-size: 1.5rem;
          margin-bottom: 4px;
        }

        .format-label {
          font-size: 0.875rem;
          font-weight: 500;
          color: #495057;
        }

        .export-format-option.active .format-label {
          color: #0d6efd;
          font-weight: 600;
        }

        .progress-bar-animated {
          animation: progress-bar-stripes 1s linear infinite;
        }

        @keyframes progress-bar-stripes {
          0% { background-position: 1rem 0; }
          100% { background-position: 0 0; }
        }
      `}</style>
    </div>
  );
};

export default ExportManagerComponent;