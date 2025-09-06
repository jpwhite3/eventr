import jsPDF from 'jspdf';
import html2canvas from 'html2canvas';
import * as XLSX from 'xlsx';
import Papa from 'papaparse';

export interface ExportOptions {
  format: 'pdf' | 'excel' | 'csv' | 'png' | 'svg';
  filename: string;
  includeCharts?: boolean;
  dateRange?: string;
  eventName?: string;
}

export interface ExportProgress {
  step: string;
  progress: number;
  total: number;
}

export type ExportProgressCallback = (progress: ExportProgress) => void;

// PDF Export utilities
export class PDFExporter {
  private pdf: jsPDF;
  private pageWidth: number;
  private pageHeight: number;
  private margin: number = 20;
  private yPosition: number = 20;

  constructor() {
    this.pdf = new jsPDF();
    this.pageWidth = this.pdf.internal.pageSize.getWidth();
    this.pageHeight = this.pdf.internal.pageSize.getHeight();
  }

  addTitle(title: string, subtitle?: string) {
    this.pdf.setFontSize(20);
    this.pdf.setFont('helvetica', 'bold');
    this.pdf.text(title, this.margin, this.yPosition);
    this.yPosition += 15;

    if (subtitle) {
      this.pdf.setFontSize(12);
      this.pdf.setFont('helvetica', 'normal');
      this.pdf.text(subtitle, this.margin, this.yPosition);
      this.yPosition += 10;
    }

    // Add export date
    this.pdf.setFontSize(10);
    this.pdf.setTextColor(128, 128, 128);
    this.pdf.text(`Generated: ${new Date().toLocaleString()}`, this.margin, this.yPosition);
    this.yPosition += 20;
    this.pdf.setTextColor(0, 0, 0);
  }

  addSection(title: string) {
    // Check if we need a new page
    if (this.yPosition > this.pageHeight - 40) {
      this.addPage();
    }

    this.pdf.setFontSize(14);
    this.pdf.setFont('helvetica', 'bold');
    this.pdf.text(title, this.margin, this.yPosition);
    this.yPosition += 12;
  }

  addKeyValuePair(key: string, value: string | number) {
    this.pdf.setFontSize(10);
    this.pdf.setFont('helvetica', 'bold');
    this.pdf.text(`${key}:`, this.margin, this.yPosition);
    
    this.pdf.setFont('helvetica', 'normal');
    this.pdf.text(String(value), this.margin + 60, this.yPosition);
    this.yPosition += 8;
  }

  addTable(headers: string[], rows: (string | number)[][]) {
    const cellWidth = (this.pageWidth - 2 * this.margin) / headers.length;
    const cellHeight = 8;

    // Check if table fits on current page
    const tableHeight = (rows.length + 1) * cellHeight;
    if (this.yPosition + tableHeight > this.pageHeight - this.margin) {
      this.addPage();
    }

    // Add headers
    this.pdf.setFont('helvetica', 'bold');
    this.pdf.setFontSize(9);
    
    headers.forEach((header, index) => {
      const x = this.margin + index * cellWidth;
      this.pdf.rect(x, this.yPosition, cellWidth, cellHeight);
      this.pdf.text(header, x + 2, this.yPosition + 5);
    });
    
    this.yPosition += cellHeight;

    // Add rows
    this.pdf.setFont('helvetica', 'normal');
    rows.forEach((row, rowIndex) => {
      // Alternate row colors
      if (rowIndex % 2 === 0) {
        this.pdf.setFillColor(245, 245, 245);
        this.pdf.rect(this.margin, this.yPosition, this.pageWidth - 2 * this.margin, cellHeight, 'F');
      }

      row.forEach((cell, cellIndex) => {
        const x = this.margin + cellIndex * cellWidth;
        this.pdf.rect(x, this.yPosition, cellWidth, cellHeight);
        this.pdf.text(String(cell), x + 2, this.yPosition + 5);
      });
      
      this.yPosition += cellHeight;

      // Check if we need a new page
      if (this.yPosition > this.pageHeight - 40) {
        this.addPage();
      }
    });

    this.yPosition += 10; // Space after table
  }

  async addChart(elementId: string, title: string) {
    const element = document.getElementById(elementId);
    if (!element) {
      console.warn(`Chart element ${elementId} not found`);
      return;
    }

    try {
      const canvas = await html2canvas(element, {
        backgroundColor: '#ffffff',
        scale: 2
      });

      const imgData = canvas.toDataURL('image/png');
      
      // Check if chart fits on current page
      const chartHeight = 80;
      if (this.yPosition + chartHeight > this.pageHeight - this.margin) {
        this.addPage();
      }

      this.addSection(title);
      
      // Add chart image
      const imgWidth = this.pageWidth - 2 * this.margin;
      const imgHeight = (canvas.height * imgWidth) / canvas.width;
      
      this.pdf.addImage(imgData, 'PNG', this.margin, this.yPosition, imgWidth, Math.min(imgHeight, chartHeight));
      this.yPosition += Math.min(imgHeight, chartHeight) + 10;
    } catch (error) {
      console.error('Error adding chart to PDF:', error);
      this.addKeyValuePair('Chart Error', 'Unable to render chart');
    }
  }

  addPage() {
    this.pdf.addPage();
    this.yPosition = this.margin;
  }

  save(filename: string) {
    this.pdf.save(filename);
  }

  getBlob(): Blob {
    return this.pdf.output('blob');
  }
}

// Excel Export utilities
export class ExcelExporter {
  private workbook: XLSX.WorkBook;

  constructor() {
    this.workbook = XLSX.utils.book_new();
  }

  addWorksheet(name: string, data: any[], headers?: string[]) {
    let worksheet: XLSX.WorkSheet;

    if (headers && data.length > 0) {
      // Create worksheet with headers
      worksheet = XLSX.utils.json_to_sheet(data, { header: headers });
    } else {
      // Auto-generate from data
      worksheet = XLSX.utils.json_to_sheet(data);
    }

    // Set column widths
    const cols = Object.keys(data[0] || {}).map(() => ({ wch: 15 }));
    worksheet['!cols'] = cols;

    XLSX.utils.book_append_sheet(this.workbook, worksheet, name);
  }

  addMetricsSheet(metrics: Record<string, any>) {
    const data = Object.entries(metrics).map(([key, value]) => ({
      Metric: key,
      Value: value,
      Type: typeof value === 'number' ? 'Number' : 'Text'
    }));

    this.addWorksheet('Key Metrics', data);
  }

  save(filename: string) {
    XLSX.writeFile(this.workbook, filename);
  }

  getBlob(): Blob {
    const wbout = XLSX.write(this.workbook, { bookType: 'xlsx', type: 'array' });
    return new Blob([wbout], { type: 'application/octet-stream' });
  }
}

// CSV Export utilities
export class CSVExporter {
  static exportData(data: any[], filename: string) {
    const csv = Papa.unparse(data, {
      header: true,
      delimiter: ',',
      newline: '\n'
    });

    this.downloadCSV(csv, filename);
  }

  static exportMultipleSheets(sheets: { name: string; data: any[] }[], filename: string) {
    let combinedCSV = '';

    sheets.forEach((sheet, index) => {
      if (index > 0) combinedCSV += '\n\n';
      combinedCSV += `=== ${sheet.name} ===\n`;
      combinedCSV += Papa.unparse(sheet.data, {
        header: true,
        delimiter: ',',
        newline: '\n'
      });
    });

    this.downloadCSV(combinedCSV, filename);
  }

  private static downloadCSV(csv: string, filename: string) {
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    
    if (link.download !== undefined) {
      const url = URL.createObjectURL(blob);
      link.setAttribute('href', url);
      link.setAttribute('download', filename);
      link.style.visibility = 'hidden';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    }
  }
}

// Chart Export utilities
export class ChartExporter {
  static async exportChartAsPNG(elementId: string, filename: string, options?: {
    width?: number;
    height?: number;
    backgroundColor?: string;
  }) {
    const element = document.getElementById(elementId);
    if (!element) {
      throw new Error(`Element ${elementId} not found`);
    }

    const canvas = await html2canvas(element, {
      backgroundColor: options?.backgroundColor || '#ffffff',
      scale: 2,
      width: options?.width,
      height: options?.height
    });

    // Convert to blob and download
    canvas.toBlob((blob) => {
      if (blob) {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
      }
    });
  }

  static exportChartAsSVG(elementId: string, filename: string) {
    const element = document.getElementById(elementId);
    if (!element) {
      throw new Error(`Element ${elementId} not found`);
    }

    // Find SVG elements in the chart
    const svgElement = element.querySelector('svg');
    if (!svgElement) {
      throw new Error('No SVG found in chart element');
    }

    const serializer = new XMLSerializer();
    const svgString = serializer.serializeToString(svgElement);
    const blob = new Blob([svgString], { type: 'image/svg+xml;charset=utf-8' });
    
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }
}

// Main Export Manager
export class ExportManager {
  static async exportAnalytics(
    analyticsData: any,
    options: ExportOptions,
    onProgress?: ExportProgressCallback
  ): Promise<void> {
    const { format, filename, includeCharts = true, eventName = 'Event' } = options;

    try {
      switch (format) {
        case 'pdf':
          await this.exportAsPDF(analyticsData, filename, includeCharts, eventName, onProgress);
          break;
        case 'excel':
          await this.exportAsExcel(analyticsData, filename, onProgress);
          break;
        case 'csv':
          await this.exportAsCSV(analyticsData, filename, onProgress);
          break;
        case 'png':
        case 'svg':
          throw new Error('PNG/SVG export should use ChartExporter directly');
        default:
          throw new Error(`Unsupported export format: ${format}`);
      }
    } catch (error) {
      console.error('Export failed:', error);
      throw error;
    }
  }

  private static async exportAsPDF(
    data: any,
    filename: string,
    includeCharts: boolean,
    eventName: string,
    onProgress?: ExportProgressCallback
  ) {
    const pdf = new PDFExporter();
    
    onProgress?.({ step: 'Initializing PDF', progress: 0, total: 6 });

    // Add title and summary
    pdf.addTitle(`${eventName} Analytics Report`, `Generated on ${new Date().toLocaleDateString()}`);
    
    onProgress?.({ step: 'Adding key metrics', progress: 1, total: 6 });

    // Add key metrics
    pdf.addSection('Key Metrics');
    if (data.totalRegistrations !== undefined) pdf.addKeyValuePair('Total Registrations', data.totalRegistrations);
    if (data.totalCheckIns !== undefined) pdf.addKeyValuePair('Total Check-ins', data.totalCheckIns);
    if (data.attendanceRate !== undefined) pdf.addKeyValuePair('Attendance Rate', `${data.attendanceRate.toFixed(1)}%`);
    if (data.revenue !== undefined) pdf.addKeyValuePair('Revenue', `$${data.revenue.toLocaleString()}`);

    onProgress?.({ step: 'Adding session data', progress: 2, total: 6 });

    // Add session analytics table
    if (data.sessionAnalytics && data.sessionAnalytics.length > 0) {
      pdf.addSection('Session Performance');
      const headers = ['Session', 'Registrations', 'Check-ins', 'Attendance Rate'];
      const rows = data.sessionAnalytics.map((session: any) => [
        session.sessionTitle || 'Unnamed Session',
        session.registrations || 0,
        session.checkedIn || 0,
        `${(session.attendanceRate || 0).toFixed(1)}%`
      ]);
      pdf.addTable(headers, rows);
    }

    onProgress?.({ step: 'Adding charts', progress: 3, total: 6 });

    // Add charts if requested
    if (includeCharts) {
      // Add registration trend chart
      await pdf.addChart('registration-trend-chart', 'Registration Timeline');
      
      // Add check-in methods chart
      await pdf.addChart('checkin-methods-chart', 'Check-in Methods Distribution');
    }

    onProgress?.({ step: 'Finalizing PDF', progress: 5, total: 6 });

    // Save the PDF
    pdf.save(filename);
    
    onProgress?.({ step: 'Complete', progress: 6, total: 6 });
  }

  private static async exportAsExcel(
    data: any,
    filename: string,
    onProgress?: ExportProgressCallback
  ) {
    const excel = new ExcelExporter();
    
    onProgress?.({ step: 'Creating Excel workbook', progress: 0, total: 4 });

    // Add key metrics sheet
    const metrics = {
      'Total Registrations': data.totalRegistrations || 0,
      'Total Check-ins': data.totalCheckIns || 0,
      'Attendance Rate (%)': data.attendanceRate || 0,
      'Revenue ($)': data.revenue || 0,
      'Session Count': data.sessionCount || 0,
      'Avg Session Attendance (%)': data.avgSessionAttendance || 0
    };
    excel.addMetricsSheet(metrics);

    onProgress?.({ step: 'Adding session data', progress: 1, total: 4 });

    // Add session analytics sheet
    if (data.sessionAnalytics && data.sessionAnalytics.length > 0) {
      excel.addWorksheet('Session Analytics', data.sessionAnalytics);
    }

    onProgress?.({ step: 'Adding registration data', progress: 2, total: 4 });

    // Add registration timeline data
    if (data.registrationsByDay && data.registrationsByDay.length > 0) {
      excel.addWorksheet('Registration Timeline', data.registrationsByDay);
    }

    // Add check-in methods data
    if (data.checkInMethods && data.checkInMethods.length > 0) {
      excel.addWorksheet('Check-in Methods', data.checkInMethods);
    }

    onProgress?.({ step: 'Saving Excel file', progress: 3, total: 4 });

    excel.save(filename);
    
    onProgress?.({ step: 'Complete', progress: 4, total: 4 });
  }

  private static async exportAsCSV(
    data: any,
    filename: string,
    onProgress?: ExportProgressCallback
  ) {
    onProgress?.({ step: 'Preparing CSV data', progress: 0, total: 2 });

    const sheets: { name: string; data: any[] }[] = [];

    // Add session analytics
    if (data.sessionAnalytics && data.sessionAnalytics.length > 0) {
      sheets.push({
        name: 'Session Analytics',
        data: data.sessionAnalytics
      });
    }

    // Add registration timeline
    if (data.registrationsByDay && data.registrationsByDay.length > 0) {
      sheets.push({
        name: 'Registration Timeline',
        data: data.registrationsByDay
      });
    }

    // Add check-in methods
    if (data.checkInMethods && data.checkInMethods.length > 0) {
      sheets.push({
        name: 'Check-in Methods',
        data: data.checkInMethods
      });
    }

    onProgress?.({ step: 'Generating CSV', progress: 1, total: 2 });

    if (sheets.length > 1) {
      CSVExporter.exportMultipleSheets(sheets, filename);
    } else if (sheets.length === 1) {
      CSVExporter.exportData(sheets[0].data, filename);
    } else {
      throw new Error('No data available for CSV export');
    }

    onProgress?.({ step: 'Complete', progress: 2, total: 2 });
  }
}