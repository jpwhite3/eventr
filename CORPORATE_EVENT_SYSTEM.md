# Corporate Event Management System

## Overview
This is an internal corporate event management system designed for organizations to create, manage, and track employee registration for company events. Unlike public event platforms, this system focuses on registration management rather than ticketing/pricing.

## 🎯 Key Features

### **For Event Organizers (Non-Technical Users)**
- **Visual Form Builder**: Drag-and-drop interface to create custom registration forms
- **Event Types**: Support for in-person, virtual, and hybrid events  
- **Registration Management**: Control approval requirements and capacity limits
- **Corporate Categories**: Business-focused event categories (Business, Technology, Education, etc.)

### **For Employees (Event Participants)**
- **Easy Registration**: Simple registration process with custom forms
- **Event Discovery**: Browse events by category, location, and date
- **Calendar Integration**: Add events to personal calendars
- **Registration Status**: Track approval status and capacity

## 🏗️ Registration Form Builder

### **Non-Technical Friendly Interface**
- **Visual Field Palette**: Click to add different field types
- **Drag & Drop Reordering**: Organize fields by dragging
- **Live Preview**: See how fields will appear to registrants
- **Field Validation**: Set required fields and validation rules

### **Supported Field Types**
- **Text Input** 📝: Name, job title, department
- **Email** 📧: Contact information
- **Phone Number** 📞: Emergency contacts
- **Number** #️⃣: Employee ID, years of service
- **Date** 📅: Birthdate, hire date
- **Multi-line Text** 📄: Comments, special requirements
- **Dropdown** 📋: Department selection, meal preferences
- **Multiple Choice** 🔘: T-shirt size, session preferences
- **Checkboxes** ☑️: Dietary restrictions, interests
- **File Upload** 📎: Headshots, resumes, documents

### **Advanced Form Features**
- **Conditional Logic**: Show/hide fields based on responses
- **Validation Rules**: Email format, phone patterns, required fields
- **Help Text**: Guide users with contextual information
- **Options Management**: Easy add/remove for dropdowns and choices

## 🎭 Event Types & Features

### **In-Person Events**
- Complete venue information (name, address, directions)
- Room/facility booking details
- Catering and logistics planning
- Capacity management for physical spaces

### **Virtual Events** 
- Meeting URLs (Zoom, Teams, WebEx)
- Dial-in numbers and access codes
- Technical requirements
- Digital resource sharing

### **Hybrid Events**
- Both physical and virtual attendance options
- Separate capacity limits for each format
- Technology requirements for remote participants
- Unified registration process

## 🔧 Registration Management

### **Approval Workflows**
- **Open Registration**: Automatic approval for all registrants
- **Approval Required**: Manual review by event organizers
- **Conditional Approval**: Based on department, role, or custom criteria

### **Capacity Control**
- Set maximum attendee limits
- Automatic waitlist management
- Real-time capacity tracking
- Overflow handling for popular events

### **Communication**
- Automated confirmation emails
- Registration status updates
- Event reminders and updates
- Last-minute changes notification

## 📊 Corporate-Focused Categories

- **Business** 💼: Leadership meetings, quarterly reviews, strategic planning
- **Technology** 💻: Tech talks, system training, IT updates
- **Education** 📚: Professional development, certifications, workshops
- **Community** 👥: Team building, volunteering, company culture
- **Health & Wellness** 🏥: Mental health sessions, fitness programs
- **Food & Drink** 🍽️: Company lunches, holiday parties, networking
- **Sports & Fitness** 🏃: Company sports leagues, fitness challenges

## 🎨 User Experience

### **Event Creation Process**
1. **Basic Information**: Title, description, category
2. **Date & Time**: Scheduling with timezone support
3. **Location Setup**: Venue details or virtual meeting info
4. **Registration Builder**: Custom form creation with visual tools
5. **Settings**: Approval requirements, capacity limits
6. **Publishing**: Make event available to employees

### **Employee Registration Flow**
1. **Event Discovery**: Browse by category or search
2. **Event Details**: View comprehensive event information
3. **Registration Form**: Complete custom form fields
4. **Confirmation**: Receive immediate status update
5. **Calendar Integration**: Add to personal calendar
6. **Updates**: Receive notifications about changes

## 🛠️ Technical Architecture

### **Backend (Kotlin/Spring Boot)**
- **Event Model**: Extended with corporate-specific fields
- **Form Definition**: JSON-based flexible form schema
- **Registration Management**: Approval workflows and capacity tracking
- **Category System**: Corporate event categorization

### **Frontend (React/TypeScript)**
- **Form Builder Component**: Visual form creation tool
- **Responsive Design**: Desktop and mobile optimized
- **Real-time Updates**: Live form preview and validation
- **Corporate Branding**: Professional UI/UX design

## 🚀 Getting Started

### **For Event Organizers**
1. Navigate to "Create Event"
2. Fill in basic event details
3. Use the Form Builder to create registration requirements
4. Set approval and capacity settings
5. Publish event for employee registration

### **For Developers**
- Backend API: Standard REST endpoints for event CRUD
- Form Schema: JSON-based field definitions
- Database: JPA entities with corporate extensions
- Frontend: React components with TypeScript

## 🎯 Future Enhancements

- **Integration**: Connect with HRIS systems, calendar platforms
- **Analytics**: Registration reports, attendance tracking
- **Templates**: Pre-built form templates for common events
- **Mobile App**: Native mobile application for employees
- **Single Sign-On**: Corporate identity provider integration
- **Reporting**: Executive dashboards and event metrics

---

**Note**: This system is designed specifically for internal corporate use and focuses on registration management rather than payment processing or public event marketing.