declare module '@coreui/react' {
  import { ComponentProps, FC, ReactNode } from 'react';
  
  interface CSidebarProps {
    visible?: boolean;
    onVisibleChange?: (visible: boolean) => void;
    className?: string;
    colorScheme?: 'light' | 'dark';
    children?: ReactNode;
  }
  
  export const CBadge: FC<any>;
  export const CButton: FC<any>;
  export const CButtonGroup: FC<any>;
  export const CCard: FC<any>;
  export const CCardBody: FC<any>;
  export const CCardHeader: FC<any>;
  export const CCol: FC<any>;
  export const CContainer: FC<any>;
  export const CDropdown: FC<any>;
  export const CDropdownToggle: FC<any>;
  export const CDropdownMenu: FC<any>;
  export const CDropdownItem: FC<any>;
  export const CHeader: FC<any>;
  export const CHeaderNav: FC<any>;
  export const CHeaderToggler: FC<any>;
  export const CNavItem: FC<any>;
  export const CNavLink: FC<any>;
  export const CNavTitle: FC<any>;
  export const CProgress: FC<any>;
  export const CRow: FC<any>;
  export const CSidebar: FC<CSidebarProps>;
  export const CSidebarNav: FC<any>;
  export const CSidebarToggler: FC<any>;
  export const CTable: FC<any>;
  export const CTableBody: FC<any>;
  export const CTableDataCell: FC<any>;
  export const CTableHead: FC<any>;
  export const CTableHeaderCell: FC<any>;
  export const CTableRow: FC<any>;
  export const CWidgetStatsF: FC<any>;
}