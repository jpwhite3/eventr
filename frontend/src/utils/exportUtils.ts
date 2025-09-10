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

export interface WorksheetFormatting {
  headerStyle?: 'basic' | 'professional' | 'pivot';
  alternateRows?: boolean;
  includeFormulas?: boolean;
  freezeHeader?: boolean;
  columnWidths?: number[];
}

export interface CSVExportOptions {
  delimiter?: ',' | ';' | '\t';
  forceQuotes?: boolean;
  includeMetadata?: boolean;
  includeSummary?: boolean;
  customHeaders?: string[];
  selectedColumns?: string[];
  dateFormat?: 'iso' | 'full' | 'short';
  dateRange?: string;
  filterInfo?: string;
}

export interface CSVDataFilter {
  column: string;
  type: 'date' | 'numeric' | 'text' | 'boolean';
  // Date filters
  startDate?: string;
  endDate?: string;
  // Numeric filters
  minValue?: number;
  maxValue?: number;
  // Text filters
  values?: string[];
  searchText?: string;
  // Boolean filters
  booleanValue?: boolean;
}

export interface ReportSchedule {
  id?: string;
  name: string;
  description?: string;
  frequency: 'daily' | 'weekly' | 'monthly' | 'quarterly';
  format: 'pdf' | 'excel' | 'csv';
  emailRecipients: string[];
  dataSource: string;
  filters?: CSVDataFilter[];
  lastGenerated?: Date;
  nextScheduled?: Date;
  active: boolean;
  template?: string;
}

// PDF Export utilities with professional branding
export class PDFExporter {
  private pdf: jsPDF;
  private pageWidth: number;
  private pageHeight: number;
  private margin: number = 20;
  private yPosition: number = 20;
  private brandColor: [number, number, number] = [0, 102, 204]; // EventR brand blue
  private headerHeight: number = 50;

  constructor() {
    this.pdf = new jsPDF();
    this.pageWidth = this.pdf.internal.pageSize.getWidth();
    this.pageHeight = this.pdf.internal.pageSize.getHeight();
    this.addBrandedHeader();
  }

  addBrandedHeader() {
    // Add EventR branded header to every page
    this.pdf.setFillColor(...this.brandColor);
    this.pdf.rect(0, 0, this.pageWidth, this.headerHeight, 'F');
    
    // EventR logo/text
    this.pdf.setTextColor(255, 255, 255);
    this.pdf.setFontSize(24);
    this.pdf.setFont('helvetica', 'bold');
    this.pdf.text('EventR', this.margin, 25);
    
    // Tagline
    this.pdf.setFontSize(10);
    this.pdf.setFont('helvetica', 'normal');
    this.pdf.text('Corporate Event Management Platform', this.margin, 35);
    
    // Add generation timestamp
    this.pdf.setTextColor(255, 255, 255);
    this.pdf.setFontSize(8);
    this.pdf.text(`Generated: ${new Date().toLocaleString()}`, this.pageWidth - 60, 35);
    
    // Reset position below header
    this.yPosition = this.headerHeight + 20;
    this.pdf.setTextColor(0, 0, 0);
  }

  addTitle(title: string, subtitle?: string) {
    // Check if we need a new page
    if (this.yPosition > this.pageHeight - 60) {
      this.addPage();
    }

    this.pdf.setFontSize(20);
    this.pdf.setFont('helvetica', 'bold');
    this.pdf.setTextColor(...this.brandColor);
    this.pdf.text(title, this.margin, this.yPosition);
    this.yPosition += 15;

    if (subtitle) {
      this.pdf.setFontSize(12);
      this.pdf.setFont('helvetica', 'normal');
      this.pdf.setTextColor(80, 80, 80);
      this.pdf.text(subtitle, this.margin, this.yPosition);
      this.yPosition += 15;
    }

    // Add separator line
    this.pdf.setDrawColor(...this.brandColor);
    this.pdf.setLineWidth(0.5);
    this.pdf.line(this.margin, this.yPosition, this.pageWidth - this.margin, this.yPosition);
    this.yPosition += 15;
    
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

  addExecutiveSummary(data: any) {
    this.addSection('📊 Executive Summary');
    
    // Key metrics in professional format
    const summaryData = [
      ['Metric', 'Current Period', 'Previous Period', 'Change'],
      ['Total Events', data.totalEvents || 0, data.previousEvents || 0, this.calculateChange(data.totalEvents, data.previousEvents)],
      ['Total Registrations', data.totalRegistrations || 0, data.previousRegistrations || 0, this.calculateChange(data.totalRegistrations, data.previousRegistrations)],
      ['Attendance Rate', `${(data.attendanceRate || 0).toFixed(1)}%`, `${(data.previousAttendanceRate || 0).toFixed(1)}%`, this.calculateChange(data.attendanceRate, data.previousAttendanceRate) + '%'],
      ['Revenue Generated', `$${(data.revenue || 0).toLocaleString()}`, `$${(data.previousRevenue || 0).toLocaleString()}`, this.calculateChange(data.revenue, data.previousRevenue, true)],
    ];
    
    this.addProfessionalTable(['Metric', 'Current', 'Previous', 'Change'], summaryData.slice(1));
    
    // Key insights
    this.addSection('🔍 Key Insights');
    const insights = this.generateInsights(data);
    insights.forEach(insight => {
      this.pdf.setFont('helvetica', 'normal');
      this.pdf.setFontSize(10);
      this.pdf.text(`• ${insight}`, this.margin + 5, this.yPosition);
      this.yPosition += 8;
    });
    this.yPosition += 10;
  }

  private calculateChange(current: number, previous: number, currency: boolean = false): string {
    if (!previous || previous === 0) return 'N/A';
    const change = ((current - previous) / previous * 100);
    const prefix = currency ? '$' : '';
    const suffix = currency ? '' : '%';
    return `${change > 0 ? '+' : ''}${prefix}${change.toFixed(1)}${suffix}`;
  }

  private generateInsights(data: any): string[] {
    const insights: string[] = [];
    
    if (data.attendanceRate && data.attendanceRate > 0.8) {
      insights.push('Strong attendance performance indicates high event value and engagement');
    }
    
    if (data.totalRegistrations && data.previousRegistrations && data.totalRegistrations > data.previousRegistrations * 1.2) {
      insights.push('Registration growth exceeding 20% indicates strong market demand');
    }
    
    if (data.sessionAnalytics && data.sessionAnalytics.length > 0) {
      const avgUtilization = data.sessionAnalytics.reduce((sum: number, s: any) => sum + (s.utilizationRate || 0), 0) / data.sessionAnalytics.length;
      if (avgUtilization > 0.9) {
        insights.push('High session utilization suggests need for capacity expansion');
      }
    }
    
    if (data.revenue && data.previousRevenue && data.revenue > data.previousRevenue) {
      insights.push('Revenue growth demonstrates successful monetization strategy');
    }
    
    // Always include at least one insight
    if (insights.length === 0) {
      insights.push('Event performance metrics are within expected ranges');
    }
    
    return insights.slice(0, 4); // Limit to 4 key insights
  }

  addProfessionalTable(headers: string[], rows: (string | number)[][]) {
    const cellWidth = (this.pageWidth - 2 * this.margin) / headers.length;
    const cellHeight = 12;

    // Check if table fits on current page
    const tableHeight = (rows.length + 2) * cellHeight;
    if (this.yPosition + tableHeight > this.pageHeight - this.margin) {
      this.addPage();
    }

    // Add professional header with brand color
    this.pdf.setFillColor(...this.brandColor);
    this.pdf.rect(this.margin, this.yPosition, this.pageWidth - 2 * this.margin, cellHeight, 'F');
    
    this.pdf.setFont('helvetica', 'bold');
    this.pdf.setFontSize(10);
    this.pdf.setTextColor(255, 255, 255);
    
    headers.forEach((header, index) => {
      const x = this.margin + index * cellWidth;
      this.pdf.text(header, x + 4, this.yPosition + 8);
    });
    
    this.yPosition += cellHeight;
    this.pdf.setTextColor(0, 0, 0);

    // Add data rows with alternating colors
    this.pdf.setFont('helvetica', 'normal');
    rows.forEach((row, rowIndex) => {
      // Alternate row colors
      if (rowIndex % 2 === 0) {
        this.pdf.setFillColor(248, 249, 250);
        this.pdf.rect(this.margin, this.yPosition, this.pageWidth - 2 * this.margin, cellHeight, 'F');
      }

      row.forEach((cell, cellIndex) => {
        const x = this.margin + cellIndex * cellWidth;
        // Add border
        this.pdf.setDrawColor(220, 220, 220);
        this.pdf.rect(x, this.yPosition, cellWidth, cellHeight);
        
        // Add text
        let displayText = String(cell);
        if (cellIndex === headers.length - 1 && displayText.includes('+')) {
          this.pdf.setTextColor(40, 167, 69); // Success green for positive changes
        } else if (cellIndex === headers.length - 1 && displayText.includes('-')) {
          this.pdf.setTextColor(220, 53, 69); // Danger red for negative changes
        }
        
        this.pdf.text(displayText, x + 4, this.yPosition + 8);
        this.pdf.setTextColor(0, 0, 0); // Reset color
      });
      
      this.yPosition += cellHeight;

      // Check if we need a new page
      if (this.yPosition > this.pageHeight - 50) {
        this.addPage();
      }
    });

    this.yPosition += 10; // Space after table
  }

  addFooter() {
    const footerY = this.pageHeight - 20;
    
    // Add footer line
    this.pdf.setDrawColor(...this.brandColor);
    this.pdf.setLineWidth(0.5);
    this.pdf.line(this.margin, footerY - 5, this.pageWidth - this.margin, footerY - 5);
    
    // Add footer text
    this.pdf.setFontSize(8);
    this.pdf.setTextColor(128, 128, 128);
    this.pdf.text('EventR - Corporate Event Management Platform', this.margin, footerY);
    this.pdf.text(`Page ${this.pdf.getCurrentPageInfo().pageNumber}`, this.pageWidth - this.margin - 20, footerY);
  }

  addPage() {
    this.pdf.addPage();
    this.addBrandedHeader();
    this.addFooter();
  }

  save(filename: string) {
    this.pdf.save(filename);
  }

  getBlob(): Blob {
    return this.pdf.output('blob');
  }
}

// Enhanced Excel Export utilities with professional formatting
export class ExcelExporter {
  private workbook: XLSX.WorkBook;
  private brandColor = 'FF0066CC'; // EventR brand color in hex

  constructor() {
    this.workbook = XLSX.utils.book_new();
  }

  addWorksheet(name: string, data: any[], headers?: string[], formatting?: WorksheetFormatting) {
    if (!data || data.length === 0) {
      // Create empty worksheet with headers if no data
      const worksheet = XLSX.utils.aoa_to_sheet([headers || ['No Data Available']]);
      XLSX.utils.book_append_sheet(this.workbook, worksheet, name);
      return;
    }

    let worksheet: XLSX.WorkSheet;

    if (headers && data.length > 0) {
      // Create worksheet with custom headers
      worksheet = XLSX.utils.json_to_sheet(data, { header: headers });
    } else {
      // Auto-generate from data
      worksheet = XLSX.utils.json_to_sheet(data);
    }

    // Apply professional formatting
    this.applyWorksheetFormatting(worksheet, data, headers, formatting);

    XLSX.utils.book_append_sheet(this.workbook, worksheet, name);
  }

  addExecutiveSheet(metrics: Record<string, any>) {
    // Create executive summary with enhanced formatting
    const executiveData = [
      ['📊 EventR Analytics Dashboard', '', '', ''],
      ['Generated:', new Date().toLocaleString(), '', ''],
      ['', '', '', ''],
      ['Key Performance Indicators', '', '', ''],
      ['Metric', 'Current Value', 'Target', 'Status'],
      ['Total Events', metrics.totalEvents || 0, metrics.targetEvents || 'N/A', this.getStatusIcon(metrics.totalEvents, metrics.targetEvents)],
      ['Total Registrations', metrics.totalRegistrations || 0, metrics.targetRegistrations || 'N/A', this.getStatusIcon(metrics.totalRegistrations, metrics.targetRegistrations)],
      ['Attendance Rate', `${(metrics.attendanceRate * 100 || 0).toFixed(1)}%`, '85%', this.getStatusIcon(metrics.attendanceRate * 100, 85)],
      ['Revenue Generated', `$${(metrics.revenue || 0).toLocaleString()}`, `$${(metrics.targetRevenue || 0).toLocaleString()}`, this.getStatusIcon(metrics.revenue, metrics.targetRevenue)],
      ['', '', '', ''],
      ['Growth Metrics', '', '', ''],
      ['Period-over-Period Growth', `${(metrics.growthRate || 0).toFixed(1)}%`, '15%', this.getStatusIcon(metrics.growthRate, 15)],
      ['Customer Satisfaction', `${(metrics.satisfaction || 0).toFixed(1)}/10`, '8.5/10', this.getStatusIcon(metrics.satisfaction, 8.5)],
    ];

    const worksheet = XLSX.utils.aoa_to_sheet(executiveData);
    
    // Set column widths for executive summary
    worksheet['!cols'] = [
      { wch: 25 }, // Metric name
      { wch: 20 }, // Current value
      { wch: 15 }, // Target
      { wch: 10 }  // Status
    ];

    // Merge cells for title
    worksheet['!merges'] = [
      { s: { r: 0, c: 0 }, e: { r: 0, c: 3 } }, // Title row
      { s: { r: 3, c: 0 }, e: { r: 3, c: 3 } }, // KPI header
      { s: { r: 10, c: 0 }, e: { r: 10, c: 3 } } // Growth metrics header
    ];

    XLSX.utils.book_append_sheet(this.workbook, worksheet, 'Executive Summary');
  }

  addTrendAnalysisSheet(trendData: any[]) {
    if (!trendData || trendData.length === 0) return;

    // Create trend analysis with calculations
    const analysisData = trendData.map((item, index) => ({
      ...item,
      'Period': index + 1,
      'Cumulative Registrations': trendData.slice(0, index + 1).reduce((sum, d) => sum + (d.registrations || 0), 0),
      'Growth Rate %': index > 0 ? (((item.registrations || 0) - (trendData[index - 1].registrations || 0)) / (trendData[index - 1].registrations || 1) * 100).toFixed(2) : '0.00',
      'Moving Average': this.calculateMovingAverage(trendData, index, 3)
    }));

    this.addWorksheet('Trend Analysis', analysisData, undefined, {
      headerStyle: 'professional',
      alternateRows: true,
      includeFormulas: true
    });
  }

  addPivotDataSheet(rawData: any[], sheetName: string = 'Pivot Data') {
    // Prepare data for pivot table creation
    const pivotReady = rawData.map(item => ({
      ...item,
      Month: new Date(item.date || Date.now()).toLocaleString('default', { month: 'long' }),
      Quarter: this.getQuarter(new Date(item.date || Date.now())),
      Year: new Date(item.date || Date.now()).getFullYear()
    }));

    this.addWorksheet(sheetName, pivotReady, undefined, {
      headerStyle: 'pivot',
      includeFormulas: false,
      freezeHeader: true
    });
  }

  private applyWorksheetFormatting(worksheet: XLSX.WorkSheet, data: any[], headers?: string[], formatting?: WorksheetFormatting) {
    const range = XLSX.utils.decode_range(worksheet['!ref'] || 'A1');
    
    // Set column widths based on content
    const colWidths = [];
    for (let C = range.s.c; C <= range.e.c; ++C) {
      let maxWidth = 10;
      for (let R = range.s.r; R <= range.e.r; ++R) {
        const cellAddress = XLSX.utils.encode_cell({ r: R, c: C });
        const cell = worksheet[cellAddress];
        if (cell && cell.v) {
          const width = String(cell.v).length;
          maxWidth = Math.max(maxWidth, Math.min(width + 2, 50));
        }
      }
      colWidths.push({ wch: maxWidth });
    }
    worksheet['!cols'] = colWidths;

    // Freeze header row if specified
    if (formatting?.freezeHeader) {
      worksheet['!freeze'] = { xSplit: 0, ySplit: 1 };
    }

    // Apply professional styling to headers
    if (formatting?.headerStyle === 'professional' || formatting?.headerStyle === 'pivot') {
      for (let C = range.s.c; C <= range.e.c; ++C) {
        const headerAddress = XLSX.utils.encode_cell({ r: 0, c: C });
        if (!worksheet[headerAddress]) continue;
        
        worksheet[headerAddress].s = {
          font: { bold: true, color: { rgb: 'FFFFFF' } },
          fill: { fgColor: { rgb: this.brandColor } },
          alignment: { horizontal: 'center', vertical: 'center' },
          border: {
            top: { style: 'thin', color: { rgb: '000000' } },
            bottom: { style: 'thin', color: { rgb: '000000' } },
            left: { style: 'thin', color: { rgb: '000000' } },
            right: { style: 'thin', color: { rgb: '000000' } }
          }
        };
      }
    }

    // Apply alternating row colors
    if (formatting?.alternateRows) {
      for (let R = range.s.r + 1; R <= range.e.r; ++R) {
        if (R % 2 === 0) {
          for (let C = range.s.c; C <= range.e.c; ++C) {
            const cellAddress = XLSX.utils.encode_cell({ r: R, c: C });
            if (!worksheet[cellAddress]) worksheet[cellAddress] = { t: 's', v: '' };
            worksheet[cellAddress].s = {
              ...worksheet[cellAddress].s,
              fill: { fgColor: { rgb: 'F8F9FA' } }
            };
          }
        }
      }
    }
  }

  private getStatusIcon(current: number, target: number): string {
    if (!target || target === 0) return '⚪';
    const percentage = (current / target) * 100;
    if (percentage >= 100) return '🟢'; // Green circle
    if (percentage >= 75) return '🟡';  // Yellow circle  
    return '🔴'; // Red circle
  }

  private calculateMovingAverage(data: any[], currentIndex: number, period: number): string {
    const startIndex = Math.max(0, currentIndex - period + 1);
    const subset = data.slice(startIndex, currentIndex + 1);
    const average = subset.reduce((sum, item) => sum + (item.registrations || 0), 0) / subset.length;
    return average.toFixed(2);
  }

  private getQuarter(date: Date): string {
    const month = date.getMonth() + 1;
    if (month <= 3) return 'Q1';
    if (month <= 6) return 'Q2';
    if (month <= 9) return 'Q3';
    return 'Q4';
  }

  addMetricsSheet(metrics: Record<string, any>) {
    this.addExecutiveSheet(metrics);
  }

  save(filename: string) {
    XLSX.writeFile(this.workbook, filename);
  }

  getBlob(): Blob {
    const wbout = XLSX.write(this.workbook, { bookType: 'xlsx', type: 'array' });
    return new Blob([wbout], { type: 'application/octet-stream' });
  }
}

// Enhanced CSV Export utilities with filtering and customization
export class CSVExporter {
  static exportData(data: any[], filename: string, options?: CSVExportOptions) {
    let processedData = this.processData(data, options);
    
    const csvConfig = {
      header: true,
      delimiter: options?.delimiter || ',',
      newline: '\n',
      quotes: options?.forceQuotes || false,
      ...(options?.customHeaders && { columns: options.customHeaders })
    };

    let csv = '';
    
    // Add professional header if requested
    if (options?.includeMetadata) {
      csv += `# EventR Analytics Export\n`;
      csv += `# Generated: ${new Date().toLocaleString()}\n`;
      csv += `# Total Records: ${processedData.length}\n`;
      if (options?.dateRange) {
        csv += `# Date Range: ${options.dateRange}\n`;
      }
      csv += `# \n`;
    }

    csv += Papa.unparse(processedData, csvConfig);

    // Add summary statistics if requested
    if (options?.includeSummary && processedData.length > 0) {
      csv += '\n\n# Summary Statistics\n';
      const numericColumns = this.identifyNumericColumns(processedData);
      numericColumns.forEach(col => {
        const values = processedData.map(row => parseFloat(row[col])).filter(v => !isNaN(v));
        if (values.length > 0) {
          csv += `# ${col} - Count: ${values.length}, Sum: ${values.reduce((a, b) => a + b, 0).toLocaleString()}, Average: ${(values.reduce((a, b) => a + b, 0) / values.length).toFixed(2)}\n`;
        }
      });
    }

    this.downloadCSV(csv, filename);
  }

  static exportMultipleSheets(sheets: { name: string; data: any[] }[], filename: string, options?: CSVExportOptions) {
    let combinedCSV = '';

    // Add professional header
    if (options?.includeMetadata) {
      combinedCSV += `# EventR Multi-Sheet Analytics Export\n`;
      combinedCSV += `# Generated: ${new Date().toLocaleString()}\n`;
      combinedCSV += `# Sheets Included: ${sheets.length}\n`;
      combinedCSV += `# Total Records: ${sheets.reduce((sum, sheet) => sum + sheet.data.length, 0)}\n`;
      combinedCSV += `# \n\n`;
    }

    sheets.forEach((sheet, index) => {
      if (index > 0) combinedCSV += '\n\n';
      combinedCSV += `# === ${sheet.name} ===\n`;
      
      // Add sheet-specific metadata
      if (options?.includeMetadata) {
        combinedCSV += `# Records: ${sheet.data.length}\n`;
        if (sheet.data.length > 0) {
          const columns = Object.keys(sheet.data[0]);
          combinedCSV += `# Columns: ${columns.join(', ')}\n`;
        }
        combinedCSV += `# \n`;
      }

      const processedData = this.processData(sheet.data, options);
      combinedCSV += Papa.unparse(processedData, {
        header: true,
        delimiter: options?.delimiter || ',',
        newline: '\n',
        quotes: options?.forceQuotes || false
      });

      // Add sheet summary
      if (options?.includeSummary && processedData.length > 0) {
        combinedCSV += '\n# Sheet Summary\n';
        const numericColumns = this.identifyNumericColumns(processedData);
        numericColumns.slice(0, 3).forEach(col => { // Limit to top 3 numeric columns
          const values = processedData.map(row => parseFloat(row[col])).filter(v => !isNaN(v));
          if (values.length > 0) {
            combinedCSV += `# ${col} Total: ${values.reduce((a, b) => a + b, 0).toLocaleString()}\n`;
          }
        });
      }
    });

    this.downloadCSV(combinedCSV, filename);
  }

  static exportFilteredData(
    data: any[], 
    filters: CSVDataFilter[], 
    filename: string, 
    options?: CSVExportOptions
  ) {
    let filteredData = [...data];

    // Apply filters
    filters.forEach(filter => {
      switch (filter.type) {
        case 'date':
          if (filter.startDate || filter.endDate) {
            filteredData = filteredData.filter(row => {
              const rowDate = new Date(row[filter.column]);
              const start = filter.startDate ? new Date(filter.startDate) : new Date('1900-01-01');
              const end = filter.endDate ? new Date(filter.endDate) : new Date('2100-12-31');
              return rowDate >= start && rowDate <= end;
            });
          }
          break;
        
        case 'numeric':
          filteredData = filteredData.filter(row => {
            const value = parseFloat(row[filter.column]);
            if (isNaN(value)) return false;
            if (filter.minValue !== undefined && value < filter.minValue) return false;
            if (filter.maxValue !== undefined && value > filter.maxValue) return false;
            return true;
          });
          break;

        case 'text':
          if (filter.values && filter.values.length > 0) {
            filteredData = filteredData.filter(row => 
              filter.values!.includes(String(row[filter.column]))
            );
          }
          if (filter.searchText) {
            filteredData = filteredData.filter(row => 
              String(row[filter.column]).toLowerCase().includes(filter.searchText!.toLowerCase())
            );
          }
          break;

        case 'boolean':
          if (filter.booleanValue !== undefined) {
            filteredData = filteredData.filter(row => 
              Boolean(row[filter.column]) === filter.booleanValue
            );
          }
          break;
      }
    });

    // Add filter information to metadata
    const enhancedOptions = {
      ...options,
      includeMetadata: true,
      filterInfo: filters.map(f => `${f.column}: ${this.getFilterDescription(f)}`).join('; ')
    };

    this.exportData(filteredData, filename, enhancedOptions);
  }

  static createBulkExport(
    datasets: { name: string; data: any[]; filters?: CSVDataFilter[] }[],
    baseFilename: string,
    options?: CSVExportOptions
  ) {
    datasets.forEach((dataset, index) => {
      const filename = `${baseFilename}_${dataset.name.replace(/\s+/g, '_').toLowerCase()}.csv`;
      
      if (dataset.filters && dataset.filters.length > 0) {
        this.exportFilteredData(dataset.data, dataset.filters, filename, options);
      } else {
        this.exportData(dataset.data, filename, options);
      }
    });
  }

  private static processData(data: any[], options?: CSVExportOptions): any[] {
    if (!data || data.length === 0) return [];

    let processed = [...data];

    // Apply column selection
    if (options?.selectedColumns && options.selectedColumns.length > 0) {
      processed = processed.map(row => {
        const newRow: any = {};
        options.selectedColumns!.forEach(col => {
          if (row.hasOwnProperty(col)) {
            newRow[col] = row[col];
          }
        });
        return newRow;
      });
    }

    // Apply data transformations
    processed = processed.map(row => {
      const newRow = { ...row };
      
      // Format dates consistently
      Object.keys(newRow).forEach(key => {
        if (newRow[key] instanceof Date) {
          newRow[key] = newRow[key].toISOString().split('T')[0];
        } else if (typeof newRow[key] === 'string' && this.isDateString(newRow[key])) {
          try {
            const date = new Date(newRow[key]);
            if (!isNaN(date.getTime())) {
              newRow[key] = options?.dateFormat === 'full' ? date.toLocaleString() : date.toISOString().split('T')[0];
            }
          } catch (e) {
            // Keep original value if date parsing fails
          }
        }
      });

      return newRow;
    });

    return processed;
  }

  private static identifyNumericColumns(data: any[]): string[] {
    if (!data || data.length === 0) return [];
    
    const firstRow = data[0];
    return Object.keys(firstRow).filter(key => {
      const sampleValues = data.slice(0, Math.min(10, data.length)).map(row => row[key]);
      const numericValues = sampleValues.filter(val => !isNaN(parseFloat(val)) && isFinite(val));
      return numericValues.length > sampleValues.length * 0.7; // At least 70% numeric
    });
  }

  private static isDateString(str: string): boolean {
    return /^\d{4}-\d{2}-\d{2}/.test(str) || /^\d{2}\/\d{2}\/\d{4}/.test(str);
  }

  private static getFilterDescription(filter: CSVDataFilter): string {
    switch (filter.type) {
      case 'date':
        let desc = '';
        if (filter.startDate) desc += `from ${filter.startDate}`;
        if (filter.endDate) desc += `${desc ? ' ' : ''}to ${filter.endDate}`;
        return desc || 'date filter';
      
      case 'numeric':
        let numDesc = '';
        if (filter.minValue !== undefined) numDesc += `>= ${filter.minValue}`;
        if (filter.maxValue !== undefined) numDesc += `${numDesc ? ', ' : ''}<= ${filter.maxValue}`;
        return numDesc || 'numeric filter';
      
      case 'text':
        if (filter.values && filter.values.length > 0) {
          return `in [${filter.values.join(', ')}]`;
        }
        if (filter.searchText) {
          return `contains "${filter.searchText}"`;
        }
        return 'text filter';
      
      case 'boolean':
        return filter.booleanValue ? 'true' : 'false';
      
      default:
        return 'custom filter';
    }
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

// Automated Reporting System
export class AutomatedReportingManager {
  private schedules: Map<string, ReportSchedule> = new Map();
  private isRunning: boolean = false;

  constructor() {
    this.loadSchedulesFromStorage();
    this.startScheduler();
  }

  addSchedule(schedule: ReportSchedule): string {
    const id = schedule.id || this.generateScheduleId();
    const fullSchedule: ReportSchedule = {
      ...schedule,
      id,
      nextScheduled: this.calculateNextRunTime(schedule.frequency),
      lastGenerated: undefined
    };
    
    this.schedules.set(id, fullSchedule);
    this.saveSchedulesToStorage();
    return id;
  }

  updateSchedule(id: string, updates: Partial<ReportSchedule>): boolean {
    const existing = this.schedules.get(id);
    if (!existing) return false;

    const updated: ReportSchedule = {
      ...existing,
      ...updates,
      id, // Ensure ID doesn't change
      nextScheduled: updates.frequency ? 
        this.calculateNextRunTime(updates.frequency) : 
        existing.nextScheduled
    };

    this.schedules.set(id, updated);
    this.saveSchedulesToStorage();
    return true;
  }

  deleteSchedule(id: string): boolean {
    const result = this.schedules.delete(id);
    if (result) {
      this.saveSchedulesToStorage();
    }
    return result;
  }

  getSchedule(id: string): ReportSchedule | undefined {
    return this.schedules.get(id);
  }

  getAllSchedules(): ReportSchedule[] {
    return Array.from(this.schedules.values());
  }

  getActiveSchedules(): ReportSchedule[] {
    return this.getAllSchedules().filter(s => s.active);
  }

  async generateScheduledReport(scheduleId: string): Promise<boolean> {
    const schedule = this.schedules.get(scheduleId);
    if (!schedule || !schedule.active) return false;

    try {
      // This would integrate with your actual data fetching logic
      const data = await this.fetchReportData(schedule.dataSource, schedule.filters);
      
      // Generate report based on format
      const filename = `${schedule.name}_${new Date().toISOString().split('T')[0]}.${schedule.format}`;
      
      switch (schedule.format) {
        case 'pdf':
          await this.generatePDFReport(data, filename, schedule);
          break;
        case 'excel':
          await this.generateExcelReport(data, filename, schedule);
          break;
        case 'csv':
          await this.generateCSVReport(data, filename, schedule);
          break;
      }

      // Send email if recipients specified
      if (schedule.emailRecipients.length > 0) {
        await this.sendReportEmail(schedule, filename);
      }

      // Update schedule
      schedule.lastGenerated = new Date();
      schedule.nextScheduled = this.calculateNextRunTime(schedule.frequency);
      this.schedules.set(scheduleId, schedule);
      this.saveSchedulesToStorage();

      return true;
    } catch (error) {
      console.error(`Failed to generate scheduled report ${scheduleId}:`, error);
      return false;
    }
  }

  private startScheduler() {
    if (this.isRunning) return;
    
    this.isRunning = true;
    
    // Check for due reports every 15 minutes
    const checkInterval = setInterval(() => {
      this.checkDueReports();
    }, 15 * 60 * 1000);

    // Store interval reference for cleanup
    (window as any).__reportSchedulerInterval = checkInterval;
  }

  private async checkDueReports() {
    const now = new Date();
    const dueSchedules = this.getActiveSchedules().filter(schedule => 
      schedule.nextScheduled && schedule.nextScheduled <= now
    );

    for (const schedule of dueSchedules) {
      await this.generateScheduledReport(schedule.id!);
    }
  }

  private calculateNextRunTime(frequency: ReportSchedule['frequency']): Date {
    const now = new Date();
    
    switch (frequency) {
      case 'daily':
        return new Date(now.getTime() + 24 * 60 * 60 * 1000);
      case 'weekly':
        return new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
      case 'monthly':
        const nextMonth = new Date(now);
        nextMonth.setMonth(nextMonth.getMonth() + 1);
        return nextMonth;
      case 'quarterly':
        const nextQuarter = new Date(now);
        nextQuarter.setMonth(nextQuarter.getMonth() + 3);
        return nextQuarter;
      default:
        return new Date(now.getTime() + 24 * 60 * 60 * 1000);
    }
  }

  private generateScheduleId(): string {
    return `schedule_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  private async fetchReportData(dataSource: string, filters?: CSVDataFilter[]): Promise<any> {
    // This would integrate with your actual API endpoints
    // For now, return mock data
    return {
      totalEvents: 150,
      totalRegistrations: 2500,
      attendanceRate: 0.85,
      revenue: 125000,
      sessionAnalytics: [],
      registrationsByDay: []
    };
  }

  private async generatePDFReport(data: any, filename: string, schedule: ReportSchedule): Promise<void> {
    const pdf = new PDFExporter();
    pdf.addTitle(`${schedule.name} - Automated Report`);
    pdf.addExecutiveSummary(data);
    
    // Add schedule information
    pdf.addSection('📅 Report Schedule Information');
    pdf.addKeyValuePair('Frequency', schedule.frequency);
    pdf.addKeyValuePair('Generated', new Date().toLocaleString());
    pdf.addKeyValuePair('Recipients', schedule.emailRecipients.join(', '));
    
    pdf.save(filename);
  }

  private async generateExcelReport(data: any, filename: string, schedule: ReportSchedule): Promise<void> {
    const excel = new ExcelExporter();
    excel.addExecutiveSheet(data);
    
    // Add schedule info sheet
    const scheduleInfo = [
      { Field: 'Report Name', Value: schedule.name },
      { Field: 'Frequency', Value: schedule.frequency },
      { Field: 'Generated', Value: new Date().toLocaleString() },
      { Field: 'Recipients', Value: schedule.emailRecipients.join(', ') },
      { Field: 'Data Source', Value: schedule.dataSource }
    ];
    excel.addWorksheet('Schedule Info', scheduleInfo);
    
    excel.save(filename);
  }

  private async generateCSVReport(data: any, filename: string, schedule: ReportSchedule): Promise<void> {
    const csvData = Object.entries(data).map(([key, value]) => ({
      Metric: key,
      Value: value,
      Generated: new Date().toISOString()
    }));

    CSVExporter.exportData(csvData, filename, {
      includeMetadata: true,
      includeSummary: true,
      dateRange: `Generated by ${schedule.name} schedule`
    });
  }

  private async sendReportEmail(schedule: ReportSchedule, filename: string): Promise<void> {
    // This would integrate with your email service
    console.log(`Would send report ${filename} to: ${schedule.emailRecipients.join(', ')}`);
    
    // In a real implementation, you would call your email API here
    // await emailService.sendReport({
    //   recipients: schedule.emailRecipients,
    //   subject: `${schedule.name} - ${new Date().toLocaleDateString()}`,
    //   body: `Your scheduled ${schedule.frequency} report is attached.`,
    //   attachment: filename
    // });
  }

  private loadSchedulesFromStorage(): void {
    try {
      const stored = localStorage.getItem('eventr_report_schedules');
      if (stored) {
        const parsed = JSON.parse(stored);
        Object.entries(parsed).forEach(([id, schedule]) => {
          this.schedules.set(id, {
            ...(schedule as ReportSchedule),
            nextScheduled: new Date((schedule as any).nextScheduled),
            lastGenerated: (schedule as any).lastGenerated ? new Date((schedule as any).lastGenerated) : undefined
          });
        });
      }
    } catch (error) {
      console.error('Failed to load report schedules from storage:', error);
    }
  }

  private saveSchedulesToStorage(): void {
    try {
      const toStore: Record<string, any> = {};
      this.schedules.forEach((schedule, id) => {
        toStore[id] = {
          ...schedule,
          nextScheduled: schedule.nextScheduled?.toISOString(),
          lastGenerated: schedule.lastGenerated?.toISOString()
        };
      });
      localStorage.setItem('eventr_report_schedules', JSON.stringify(toStore));
    } catch (error) {
      console.error('Failed to save report schedules to storage:', error);
    }
  }

  stopScheduler(): void {
    this.isRunning = false;
    if ((window as any).__reportSchedulerInterval) {
      clearInterval((window as any).__reportSchedulerInterval);
      delete (window as any).__reportSchedulerInterval;
    }
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
    
    onProgress?.({ step: 'Initializing professional PDF report', progress: 0, total: 8 });

    // Add professional title with branding
    pdf.addTitle(`${eventName} Analytics Report`, `Comprehensive Performance Analysis - ${new Date().toLocaleDateString()}`);
    
    onProgress?.({ step: 'Creating executive summary', progress: 1, total: 8 });

    // Add executive summary with insights
    pdf.addExecutiveSummary(data);
    
    onProgress?.({ step: 'Adding detailed metrics', progress: 2, total: 8 });

    // Add key metrics in professional format
    if (data.totalRegistrations !== undefined || data.totalCheckIns !== undefined) {
      pdf.addSection('📊 Performance Metrics');
      const metricsData = [];
      
      if (data.totalRegistrations !== undefined) {
        metricsData.push(['Total Registrations', data.totalRegistrations, data.targetRegistrations || 'N/A', this.getPerformanceIndicator(data.totalRegistrations, data.targetRegistrations)]);
      }
      if (data.totalCheckIns !== undefined) {
        metricsData.push(['Total Check-ins', data.totalCheckIns, data.expectedCheckIns || 'N/A', this.getPerformanceIndicator(data.totalCheckIns, data.expectedCheckIns)]);
      }
      if (data.attendanceRate !== undefined) {
        metricsData.push(['Attendance Rate', `${(data.attendanceRate * 100).toFixed(1)}%`, '85%', this.getPerformanceIndicator(data.attendanceRate * 100, 85)]);
      }
      if (data.revenue !== undefined) {
        metricsData.push(['Revenue Generated', `$${data.revenue.toLocaleString()}`, `$${(data.targetRevenue || 0).toLocaleString()}`, this.getPerformanceIndicator(data.revenue, data.targetRevenue)]);
      }

      if (metricsData.length > 0) {
        pdf.addProfessionalTable(['Metric', 'Actual', 'Target', 'Status'], metricsData);
      }
    }

    onProgress?.({ step: 'Adding session analysis', progress: 3, total: 8 });

    // Add session analytics with enhanced formatting
    if (data.sessionAnalytics && data.sessionAnalytics.length > 0) {
      pdf.addSection('📅 Session Performance Analysis');
      const headers = ['Session Title', 'Registrations', 'Check-ins', 'Attendance Rate', 'Utilization'];
      const rows = data.sessionAnalytics.map((session: any) => [
        session.sessionTitle || 'Unnamed Session',
        session.registrations || 0,
        session.checkedIn || 0,
        `${((session.attendanceRate || 0) * 100).toFixed(1)}%`,
        `${((session.utilizationRate || 0) * 100).toFixed(1)}%`
      ]);
      pdf.addProfessionalTable(headers, rows);
    }

    onProgress?.({ step: 'Adding trend analysis', progress: 4, total: 8 });

    // Add registration trends
    if (data.registrationsByDay && data.registrationsByDay.length > 0) {
      pdf.addSection('📈 Registration Trends');
      const trendHeaders = ['Date', 'Daily Registrations', 'Cumulative Total', 'Growth Rate'];
      const trendRows = data.registrationsByDay.map((day: any, index: number) => {
        const cumulative = data.registrationsByDay.slice(0, index + 1).reduce((sum: number, d: any) => sum + (d.registrations || 0), 0);
        const growthRate = index > 0 ? 
          (((day.registrations || 0) - (data.registrationsByDay[index - 1].registrations || 0)) / (data.registrationsByDay[index - 1].registrations || 1) * 100).toFixed(1) + '%' 
          : '0%';
        
        return [
          day.date || 'Unknown',
          day.registrations || 0,
          cumulative,
          growthRate
        ];
      });
      pdf.addProfessionalTable(trendHeaders, trendRows.slice(-10)); // Show last 10 days
    }

    onProgress?.({ step: 'Adding visual charts', progress: 5, total: 8 });

    // Add charts with professional styling
    if (includeCharts) {
      await pdf.addChart('registration-trend-chart', '📈 Registration Timeline Analysis');
      await pdf.addChart('checkin-methods-chart', '🔍 Check-in Methods Distribution');
      
      // Add session analytics chart if element exists
      const sessionChartElement = document.getElementById('session-analytics-chart');
      if (sessionChartElement) {
        await pdf.addChart('session-analytics-chart', '📊 Session Performance Overview');
      }
    }

    onProgress?.({ step: 'Adding recommendations', progress: 6, total: 8 });

    // Add business recommendations
    pdf.addSection('💡 Strategic Recommendations');
    const recommendations = this.generateRecommendations(data);
    recommendations.forEach(rec => {
      pdf.addKeyValuePair('•', rec);
    });

    onProgress?.({ step: 'Finalizing professional report', progress: 7, total: 8 });

    // Add footer to all pages
    pdf.addFooter();

    // Save the professional PDF
    pdf.save(filename);
    
    onProgress?.({ step: 'Professional PDF report completed', progress: 8, total: 8 });
  }

  private static getPerformanceIndicator(actual: number, target: number): string {
    if (!target || target === 0) return '—';
    const percentage = (actual / target) * 100;
    if (percentage >= 100) return '🟢 Exceeds';
    if (percentage >= 90) return '🟡 Meets';
    if (percentage >= 75) return '🟠 Below';
    return '🔴 Critical';
  }

  private static generateRecommendations(data: any): string[] {
    const recommendations: string[] = [];
    
    if (data.attendanceRate && data.attendanceRate < 0.75) {
      recommendations.push('Consider improving event communication and reminder systems to boost attendance rates');
    }
    
    if (data.sessionAnalytics && data.sessionAnalytics.length > 0) {
      const lowPerformingSessions = data.sessionAnalytics.filter((s: any) => (s.attendanceRate || 0) < 0.6);
      if (lowPerformingSessions.length > 0) {
        recommendations.push(`Review content and scheduling for ${lowPerformingSessions.length} underperforming sessions`);
      }
    }
    
    if (data.revenue && data.targetRevenue && data.revenue < data.targetRevenue * 0.9) {
      recommendations.push('Explore additional revenue opportunities through premium offerings or sponsorships');
    }
    
    if (data.registrationsByDay && data.registrationsByDay.length > 3) {
      const recentTrend = data.registrationsByDay.slice(-3);
      const isDecreasing = recentTrend.every((day: any, idx: number) => 
        idx === 0 || day.registrations < recentTrend[idx - 1].registrations
      );
      if (isDecreasing) {
        recommendations.push('Registration momentum is declining - consider targeted marketing campaigns');
      }
    }
    
    // Ensure at least one recommendation
    if (recommendations.length === 0) {
      recommendations.push('Continue monitoring key performance indicators and maintain current operational excellence');
    }
    
    return recommendations;
  }

  private static async exportAsExcel(
    data: any,
    filename: string,
    onProgress?: ExportProgressCallback
  ) {
    const excel = new ExcelExporter();
    
    onProgress?.({ step: 'Creating professional Excel workbook', progress: 0, total: 6 });

    // Add executive summary with enhanced metrics
    const metrics = {
      totalEvents: data.totalEvents || 0,
      totalRegistrations: data.totalRegistrations || 0,
      totalCheckIns: data.totalCheckIns || 0,
      attendanceRate: data.attendanceRate || 0,
      revenue: data.revenue || 0,
      sessionCount: data.sessionCount || 0,
      avgSessionAttendance: data.avgSessionAttendance || 0,
      targetRegistrations: data.targetRegistrations,
      targetRevenue: data.targetRevenue,
      growthRate: data.growthRate || 0,
      satisfaction: data.satisfaction || 0
    };
    excel.addExecutiveSheet(metrics);

    onProgress?.({ step: 'Adding detailed session analytics', progress: 1, total: 6 });

    // Add session analytics with enhanced formatting
    if (data.sessionAnalytics && data.sessionAnalytics.length > 0) {
      const enhancedSessionData = data.sessionAnalytics.map((session: any, index: number) => ({
        'Rank': index + 1,
        'Session Title': session.sessionTitle || 'Unnamed Session',
        'Registrations': session.registrations || 0,
        'Check-ins': session.checkedIn || 0,
        'Attendance Rate': `${((session.attendanceRate || 0) * 100).toFixed(1)}%`,
        'Utilization Rate': `${((session.utilizationRate || 0) * 100).toFixed(1)}%`,
        'Capacity': session.capacity || 0,
        'Waitlist Count': session.waitlistCount || 0,
        'Performance Score': this.calculateSessionScore(session),
        'Status': this.getSessionStatus(session)
      }));
      
      excel.addWorksheet('Session Performance', enhancedSessionData, undefined, {
        headerStyle: 'professional',
        alternateRows: true,
        freezeHeader: true
      });
    }

    onProgress?.({ step: 'Adding trend analysis data', progress: 2, total: 6 });

    // Add registration timeline with trend analysis
    if (data.registrationsByDay && data.registrationsByDay.length > 0) {
      excel.addTrendAnalysisSheet(data.registrationsByDay);
    }

    onProgress?.({ step: 'Adding operational data sheets', progress: 3, total: 6 });

    // Add check-in methods with analysis
    if (data.checkInMethods && data.checkInMethods.length > 0) {
      const enhancedCheckInData = data.checkInMethods.map((method: any) => ({
        'Check-in Method': method.method || 'Unknown',
        'Count': method.count || 0,
        'Percentage': `${((method.percentage || 0) * 100).toFixed(1)}%`,
        'Efficiency Rating': this.getEfficiencyRating(method.method),
        'Recommendation': this.getMethodRecommendation(method)
      }));
      
      excel.addWorksheet('Check-in Analysis', enhancedCheckInData, undefined, {
        headerStyle: 'professional',
        alternateRows: true
      });
    }

    // Add revenue analysis if available
    if (data.revenue && data.revenue > 0) {
      const revenueData = [
        { Category: 'Registration Fees', Amount: data.revenue * 0.8, Percentage: '80%' },
        { Category: 'Sponsorships', Amount: data.revenue * 0.15, Percentage: '15%' },
        { Category: 'Additional Services', Amount: data.revenue * 0.05, Percentage: '5%' }
      ];
      
      excel.addWorksheet('Revenue Breakdown', revenueData, undefined, {
        headerStyle: 'professional',
        alternateRows: true
      });
    }

    onProgress?.({ step: 'Adding pivot-ready data', progress: 4, total: 6 });

    // Create pivot-ready comprehensive data
    const pivotData = this.preparePivotData(data);
    if (pivotData.length > 0) {
      excel.addPivotDataSheet(pivotData, 'Comprehensive Data');
    }

    onProgress?.({ step: 'Finalizing professional Excel workbook', progress: 5, total: 6 });

    excel.save(filename);
    
    onProgress?.({ step: 'Professional Excel export completed', progress: 6, total: 6 });
  }

  private static calculateSessionScore(session: any): number {
    const attendance = (session.attendanceRate || 0) * 100;
    const utilization = (session.utilizationRate || 0) * 100;
    const hasWaitlist = (session.waitlistCount || 0) > 0 ? 10 : 0;
    
    return Math.round((attendance * 0.4) + (utilization * 0.4) + (hasWaitlist * 0.2));
  }

  private static getSessionStatus(session: any): string {
    const score = this.calculateSessionScore(session);
    if (score >= 90) return 'Excellent';
    if (score >= 75) return 'Good';
    if (score >= 60) return 'Average';
    if (score >= 40) return 'Below Average';
    return 'Needs Improvement';
  }

  private static getEfficiencyRating(method: string): string {
    const ratings: Record<string, string> = {
      'QR Code': 'Excellent',
      'Mobile App': 'Very Good',
      'Online': 'Good',
      'Manual': 'Fair',
      'Walk-in': 'Basic'
    };
    return ratings[method] || 'Unknown';
  }

  private static getMethodRecommendation(method: any): string {
    if (method.method === 'Manual' && (method.percentage || 0) > 0.3) {
      return 'Consider digital alternatives to reduce manual processing';
    }
    if (method.method === 'QR Code' && (method.percentage || 0) < 0.5) {
      return 'Promote QR code usage for faster check-ins';
    }
    if (method.method === 'Walk-in' && (method.percentage || 0) > 0.2) {
      return 'Implement pre-registration incentives to reduce walk-ins';
    }
    return 'Maintain current performance';
  }

  private static preparePivotData(data: any): any[] {
    const pivotData = [];
    
    // Add session-based records
    if (data.sessionAnalytics && data.sessionAnalytics.length > 0) {
      data.sessionAnalytics.forEach((session: any) => {
        pivotData.push({
          'Record Type': 'Session',
          'Name': session.sessionTitle || 'Unnamed Session',
          'Registrations': session.registrations || 0,
          'Check-ins': session.checkedIn || 0,
          'Attendance Rate': session.attendanceRate || 0,
          'Capacity': session.capacity || 0,
          'Category': 'Performance',
          'Date': new Date().toISOString().split('T')[0]
        });
      });
    }

    // Add daily registration records
    if (data.registrationsByDay && data.registrationsByDay.length > 0) {
      data.registrationsByDay.forEach((day: any) => {
        pivotData.push({
          'Record Type': 'Daily Registration',
          'Name': 'Daily Total',
          'Registrations': day.registrations || 0,
          'Check-ins': 0,
          'Attendance Rate': 0,
          'Capacity': 0,
          'Category': 'Timeline',
          'Date': day.date || new Date().toISOString().split('T')[0]
        });
      });
    }

    return pivotData;
  }

  private static async exportAsCSV(
    data: any,
    filename: string,
    onProgress?: ExportProgressCallback
  ) {
    onProgress?.({ step: 'Preparing professional CSV export', progress: 0, total: 3 });

    const sheets: { name: string; data: any[] }[] = [];

    // Add comprehensive session analytics
    if (data.sessionAnalytics && data.sessionAnalytics.length > 0) {
      const enhancedSessions = data.sessionAnalytics.map((session: any, index: number) => ({
        'Rank': index + 1,
        'Session_Title': session.sessionTitle || 'Unnamed Session',
        'Registrations': session.registrations || 0,
        'Check_Ins': session.checkedIn || 0,
        'Attendance_Rate_Percent': ((session.attendanceRate || 0) * 100).toFixed(2),
        'Utilization_Rate_Percent': ((session.utilizationRate || 0) * 100).toFixed(2),
        'Capacity': session.capacity || 0,
        'Waitlist_Count': session.waitlistCount || 0,
        'Performance_Score': this.calculateSessionScore(session),
        'Status': this.getSessionStatus(session),
        'Export_Date': new Date().toISOString().split('T')[0]
      }));
      
      sheets.push({
        name: 'Session Performance Analytics',
        data: enhancedSessions
      });
    }

    // Add registration timeline with analysis
    if (data.registrationsByDay && data.registrationsByDay.length > 0) {
      const enhancedTimeline = data.registrationsByDay.map((day: any, index: number) => {
        const cumulative = data.registrationsByDay.slice(0, index + 1)
          .reduce((sum: number, d: any) => sum + (d.registrations || 0), 0);
        const growthRate = index > 0 ? 
          (((day.registrations || 0) - (data.registrationsByDay[index - 1].registrations || 0)) / 
           (data.registrationsByDay[index - 1].registrations || 1) * 100).toFixed(2) : '0.00';
        
        return {
          'Date': day.date || new Date().toISOString().split('T')[0],
          'Daily_Registrations': day.registrations || 0,
          'Cumulative_Total': cumulative,
          'Growth_Rate_Percent': growthRate,
          'Day_of_Week': new Date(day.date || Date.now()).toLocaleDateString('en-US', { weekday: 'long' }),
          'Week_Number': this.getWeekNumber(new Date(day.date || Date.now())),
          'Month': new Date(day.date || Date.now()).toLocaleDateString('en-US', { month: 'long' }),
          'Quarter': this.getQuarter(new Date(day.date || Date.now()))
        };
      });
      
      sheets.push({
        name: 'Registration Timeline Analysis',
        data: enhancedTimeline
      });
    }

    onProgress?.({ step: 'Adding operational and financial data', progress: 1, total: 3 });

    // Add check-in methods with recommendations
    if (data.checkInMethods && data.checkInMethods.length > 0) {
      const enhancedCheckIns = data.checkInMethods.map((method: any) => ({
        'Check_In_Method': method.method || 'Unknown',
        'Usage_Count': method.count || 0,
        'Usage_Percentage': ((method.percentage || 0) * 100).toFixed(2),
        'Efficiency_Rating': this.getEfficiencyRating(method.method),
        'Speed_Category': this.getSpeedCategory(method.method),
        'Recommendation': this.getMethodRecommendation(method),
        'Digital_Method': this.isDigitalMethod(method.method) ? 'Yes' : 'No'
      }));
      
      sheets.push({
        name: 'Check-in Method Analysis',
        data: enhancedCheckIns
      });
    }

    // Add executive summary data
    const executiveData = [{
      'Metric': 'Total Events',
      'Value': data.totalEvents || 0,
      'Target': data.targetEvents || 'N/A',
      'Performance': this.getPerformanceIndicator(data.totalEvents, data.targetEvents).replace(/🟢|🟡|🟠|🔴/g, '').trim()
    }, {
      'Metric': 'Total Registrations',
      'Value': data.totalRegistrations || 0,
      'Target': data.targetRegistrations || 'N/A',
      'Performance': this.getPerformanceIndicator(data.totalRegistrations, data.targetRegistrations).replace(/🟢|🟡|🟠|🔴/g, '').trim()
    }, {
      'Metric': 'Attendance Rate',
      'Value': `${((data.attendanceRate || 0) * 100).toFixed(2)}%`,
      'Target': '85%',
      'Performance': this.getPerformanceIndicator((data.attendanceRate || 0) * 100, 85).replace(/🟢|🟡|🟠|🔴/g, '').trim()
    }, {
      'Metric': 'Revenue Generated',
      'Value': `$${(data.revenue || 0).toLocaleString()}`,
      'Target': `$${(data.targetRevenue || 0).toLocaleString()}`,
      'Performance': this.getPerformanceIndicator(data.revenue, data.targetRevenue).replace(/🟢|🟡|🟠|🔴/g, '').trim()
    }];

    sheets.push({
      name: 'Executive Summary',
      data: executiveData
    });

    onProgress?.({ step: 'Generating comprehensive CSV files', progress: 2, total: 3 });

    // Export with professional options
    const csvOptions: CSVExportOptions = {
      includeMetadata: true,
      includeSummary: true,
      dateFormat: 'iso',
      dateRange: 'Analytics Export Generated'
    };

    CSVExporter.exportMultipleSheets(sheets, filename, csvOptions);

    onProgress?.({ step: 'Professional CSV export completed', progress: 3, total: 3 });
  }

  private static getWeekNumber(date: Date): number {
    const firstDayOfYear = new Date(date.getFullYear(), 0, 1);
    const pastDaysOfYear = (date.getTime() - firstDayOfYear.getTime()) / 86400000;
    return Math.ceil((pastDaysOfYear + firstDayOfYear.getDay() + 1) / 7);
  }

  private static getSpeedCategory(method: string): string {
    const speedMap: Record<string, string> = {
      'QR Code': 'Fast',
      'Mobile App': 'Fast',
      'Online': 'Medium',
      'Manual': 'Slow',
      'Walk-in': 'Medium'
    };
    return speedMap[method] || 'Unknown';
  }

  private static isDigitalMethod(method: string): boolean {
    return ['QR Code', 'Mobile App', 'Online'].includes(method);
  }
}