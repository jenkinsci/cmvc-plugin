package hudson.plugins.cmvc;

import hudson.model.AbstractBuild;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.TransformerUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Represents a set of changes
 * 
 * @author <a href="mailto:fuechi@ciandt.com">Fábio Franco Uechi</a>
 *
 */
public final class CmvcChangeLogSet extends ChangeLogSet<CmvcChangeLogSet.CmvcChangeLog> {

    private List<CmvcChangeLog> logs;
    
    /**
     * Holds the correspondent track names
     */
    private Set<String> trackNames;
    
    
    public CmvcChangeLogSet(AbstractBuild<?,?> build) {
        super(build);
    }
    
    public CmvcChangeLogSet(AbstractBuild<?,?> build, List<CmvcChangeLog> logs) {
        super(build);
        this.logs = Collections.unmodifiableList(logs);
    }
    
    @Override
    public boolean isEmptySet() {
        return logs == null || logs.isEmpty();
    }

    public Iterator<CmvcChangeLog> iterator() {
        return logs.iterator();
    }

	public List<CmvcChangeLog> getLogs() {
		return logs;
	}

	public void setLogs(List<CmvcChangeLog> logs) {
		this.logs = logs;
	}
    
	public Set<String> getTrackNames() {
		return trackNames;
	}

	public void setTrackNames(Set<String> trackNames) {
		this.trackNames = trackNames;
	}
	
	/**
	 * @param releaseName 
	 * @return A set of tracks associated with releaseName.
	 */
	public Set<String> getTracksPerRelease(String releaseName) {
		Set<String> tracks = new HashSet<String>();
		
		List<CmvcChangeLog> lstCmvcChangeLog = getLogs();
		for ( CmvcChangeLog changeLog : lstCmvcChangeLog ) {
			if ( releaseName.equals(changeLog.getReleaseName()) ) {
				tracks.add(changeLog.getTrackName());
			}
		}
		
		return tracks;
	}

    /**
     * In-memory representation of CMVC Changelog: defect or feature.
     */
	@ExportedBean(defaultVisibility=999)
    public static class CmvcChangeLog extends ChangeLogSet.Entry {
        
    	public CmvcChangeLog() {
		}
    	
    	public CmvcChangeLog(CmvcChangeLogSet changeLogSet) {
			setParent(changeLogSet);
		}

		/**
    	 * Modification´s date and time 
    	 */
    	private Date dateTime;
        
        /**
         * Modification´s author
         */
        private User author;
        
        /**
         * Comment
         */
        private String msg;
        
        /**
         * Modification´s type: feature (f) or defect (d)
         */
        private String type;
        
        /**
         * Level name
         */
        private String level;
        
        /**
         * Track name
         */
        private String trackName;
        
        /**
         * Release name
         */
        private String releaseName;
        
        
		@Exported
        public String getReleaseName() {
			return releaseName;
		}

		public void setReleaseName(String releaseName) {
			this.releaseName = releaseName;
		}

		@Exported
        public String getTrackName() {
			return trackName;
		}

		public void setTrackName(String trackName) {
			this.trackName = trackName;
		}

		/**
         * List of modified files within this change set 
         */
        private List<ModifiedFile> files = new ArrayList<ModifiedFile>();
        
        public void addFile(ModifiedFile file) {
        	if (file != null) {
        		file.setParent(this);
        		this.files.add(file);
        	}
        }
        
        @SuppressWarnings("unchecked")
		@Override
		public Collection<String> getAffectedPaths() {
			return CollectionUtils.collect(files, TransformerUtils
					.invokerTransformer("getPath"));
		}
        
        public void setUser(String author) {
        	if (StringUtils.isNotEmpty(author))
        		this.author = User.get(author);
        }

        @Exported
        public String getUser() {
            return author.getDisplayName();
        }

        @Exported
        public Date getDateTime() {
            return dateTime;
        }

        public void setDateTime(Date date) {
            this.dateTime = date;
        }

        @Override @Exported
        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }        
        @Override
        public User getAuthor() {
            if(author==null)
                return User.getUnknown();
            return author;
        }
        
        @Exported
        public String getType() {
        	return type;
        }
        public void setType(String type) {
        	this.type = type;
        }
        
        @Exported
        public String getLevel() {
        	return level;
        }
        public void setLevel(String level) {
        	this.level = level;
        }
        
		public List<ModifiedFile> getFiles() {
			return files;
		}

		public void setFiles(List<ModifiedFile> files) {
			this.files = files;
		}
		
        /**
         * Represent a modified file
         * 
         * @author fuechi
         */
        @ExportedBean(defaultVisibility=999)
        public static class ModifiedFile {
            
        	/**
        	 * File´s path  
        	 */
        	private String path;
        	
            /**
             * Action performed upon this file: delete, create, delta, link
             */
            private String action;
            
            /**
             * File´s version
             */
            private String version;
            
            /**
             * 
             */
            private CmvcChangeLog parent;

            public ModifiedFile() {
                this("","","");
            }
            
            public ModifiedFile(String path, String action, String version) {
                this.path = path;
                this.action = action;
                this.version = version;
            }

            public CmvcChangeLog getParent() {
                return parent;
            }

            void setParent(CmvcChangeLog parent) {
                this.parent = parent;
            }

            @Exported
            public String getPath() {
                return path;
            }

            public void setPath(String path) {
                this.path = path;
            }

            @Exported   
            public String getAction() {
                return action;
            }

            public void setAction(String action) {
                this.action = action;
            }

            @Exported
            public EditType getEditType() {
                if (action.equalsIgnoreCase("delete")) {
                    return EditType.DELETE;
                }
                if (action.equalsIgnoreCase("add") || action.equalsIgnoreCase("create") ) {
                    return EditType.ADD;
                }
                // for "delta" and "link"
                return EditType.EDIT;
            }

			public String getVersion() {
				return version;
			}

			public void setVersion(String version) {
				this.version = version;
			}
            
        }


    }

}
